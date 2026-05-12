import polars as pl
import numpy as np
import sqlite3
import os
import gc
import duckdb
import pandas as pd
import torch
import torch.nn.functional as F
print(duckdb.__version__)
print("thay đổi 1")

class UserVectorBuilder:    
    @staticmethod
    def build_user_history(interactions_path: str, item_index_path: str, output_db_path: str):
        """
        Sử dụng DuckDB Out-of-Core Engine để xử lý dữ liệu khổng lồ mà không lo tràn RAM.
        Tự động tràn dữ liệu ra ổ đĩa (Disk-spilling) nếu vượt quá giới hạn RAM.
        """
        print("Initializing DuckDB Engine for User History...")
        
        # Đảm bảo thư mục đích tồn tại
        os.makedirs(os.path.dirname(os.path.abspath(output_db_path)), exist_ok=True)
        
        # Nếu file DB đã tồn tại, xóa đi để làm lại từ đầu
        if os.path.exists(output_db_path):
            os.remove(output_db_path)
            
        # Khởi tạo kết nối trực tiếp đến file đích (Persistent Database)
        conn = duckdb.connect(output_db_path)
        
        # THIẾT LẬP TỐI QUAN TRỌNG: Vũ khí chống tràn RAM
        conn.execute("PRAGMA threads=4")
        conn.execute("PRAGMA memory_limit='8GB'") # Khóa trần RAM ở mức 4GB
        
        # Thư mục tạm để DuckDB xả rác nếu RAM đầy (Disk Spilling)
        temp_dir = os.path.join(os.path.dirname(output_db_path), "duckdb_tmp")
        os.makedirs(temp_dir, exist_ok=True)
        conn.execute(f"PRAGMA temp_directory='{temp_dir}'")
        
        # Xác định lệnh đọc dựa trên đuôi file
        read_inter_cmd = f"read_parquet('{interactions_path}')" if interactions_path.endswith('.parquet') else f"read_csv_auto('{interactions_path}')"
        read_item_cmd = f"read_parquet('{item_index_path}')" if item_index_path.endswith('.parquet') else f"read_csv_auto('{item_index_path}')"

        print(f"Đang thực thi siêu truy vấn xử lý dữ liệu và dội xuống đĩa...")
        
        # Thực thi một câu lệnh SQL duy nhất: Đọc -> Join -> Tính toán -> Gom nhóm -> Ghi đĩa
        sql_query = f"""
            CREATE TABLE user_history AS 
            WITH joined_data AS (
                SELECT 
                    i.user_id,
                    idx.row_idx AS item_idx,
                    i.rating,
                    -- Xác định Explicit
                    CASE WHEN i.rating IS NOT NULL AND i.rating > 0 THEN true ELSE false END AS is_explicit,
                    -- Tính trọng số dịch tâm
                    CASE 
                        WHEN i.rating IS NOT NULL AND i.rating > 0 THEN i.rating - 3.0 
                        ELSE 0.5 
                    END AS weight
                FROM {read_inter_cmd} i
                INNER JOIN {read_item_cmd} idx ON CAST(i.book_id AS VARCHAR) = CAST(idx.book_id AS VARCHAR)
            ),
            aggregated_data AS (
                SELECT 
                    user_id,
                    CAST(SUM(ABS(weight)) AS FLOAT) AS total_abs_weight,
                    CAST(COUNT(*) AS INTEGER) AS total,
                    -- Gom các item_idx thành dạng danh sách (List) trực tiếp
                    LIST(item_idx) AS seen_books,
                    LIST(item_idx) FILTER (WHERE is_explicit) AS rated_books
                FROM joined_data
                GROUP BY user_id
            )
            SELECT 
                -- Tự động sinh user_idx bằng Window Function
                CAST(ROW_NUMBER() OVER(ORDER BY user_id) - 1 AS INTEGER) AS user_idx,
                user_id,
                total_abs_weight,
                seen_books,
                rated_books,
                total
            FROM aggregated_data;
        """
        
        # Bắt đầu chạy. Nếu RAM đầy, DuckDB sẽ tự ghi ra thư mục temp_dir.
        conn.execute(sql_query)
        
        # Tạo Index cho user_id và user_idx để tra cứu Online siêu tốc
        print("Creating Index for lookup...")
        conn.execute("CREATE UNIQUE INDEX idx_user_id ON user_history(user_id)")
        conn.execute("CREATE UNIQUE INDEX idx_user_idx ON user_history(user_idx)")
        
        # Xả bộ nhớ và dọn dẹp thư mục tạm
        conn.close()
        try:
            for f in os.listdir(temp_dir):
                os.remove(os.path.join(temp_dir, f))
            os.rmdir(temp_dir)
        except Exception:
            pass
            
        print(f"[Success] User history has been packed by DuckDB at: {output_db_path}")
    def export_user_index_csv(db_path: str, output_csv_path: str):
        print(f"Exporting universal User Index to {output_csv_path}...")
        
        if not os.path.exists(db_path):
            raise FileNotFoundError(f"Database not found at {db_path}. Please run build_user_history_duckdb first.")
            
        # Ensure output directory exists
        os.makedirs(os.path.dirname(os.path.abspath(output_csv_path)), exist_ok=True)
        
        # Connect to the existing DuckDB database (read-only mode is safer)
        conn = duckdb.connect(db_path, read_only=True)
        
        try:
            # Use DuckDB's highly optimized native COPY command to write directly to disk
            copy_query = f"""
                COPY (
                    SELECT user_id, user_idx 
                    FROM user_history 
                    ORDER BY user_idx
                ) TO '{output_csv_path}' (HEADER, DELIMITER ',');
            """
            conn.execute(copy_query)
            print(f"[Success] Universal User Index successfully saved to {output_csv_path}")
            
        except Exception as e:
            print(f"[Error] Failed to export CSV: {e}")
            raise
        finally:
            conn.close()


    @staticmethod
    def build_user_matrix_gpu_standalone(
        interactions_path: str, 
        item_matrix_path: str, 
        user_index_path: str, 
        item_index_path: str,
        output_matrix_path: str,
        chunk_size: int = 2_000_000
    ):
        """
        Builds user embeddings using GPU. Completely decoupled from SQLite history.
        Calculates and accumulates total absolute weights dynamically in VRAM.
        """
        if not torch.cuda.is_available():
            raise RuntimeError("CUDA is not available. GPU is required.")
            
        device = torch.device('cuda')
        print(f"Using GPU device: {torch.cuda.get_device_name(0)}")
        
        # 1. Load Item Matrix to GPU
        print(f"Loading Item Matrix from {item_matrix_path} to GPU...")
        item_matrix_cpu = np.load(item_matrix_path)
        item_matrix_gpu = torch.tensor(item_matrix_cpu, device=device, dtype=torch.float32)
        vector_dim = item_matrix_gpu.shape[1]
        del item_matrix_cpu
        
        # 2. Load Mapping Indexes
        print("Loading Mapping Indexes into RAM...")
        df_user = pd.read_csv(user_index_path)
        df_item = pd.read_csv(item_index_path)
        
        if 'row_idx' in df_item.columns:
            df_item.rename(columns={'row_idx': 'item_idx'}, inplace=True)
            
        num_users = len(df_user)
        print(f"Allocating Matrices on GPU for {num_users:,} users...")
        
        # TARGET 1: The Vector Matrix
        user_matrix_gpu = torch.zeros((num_users, vector_dim), device=device, dtype=torch.float32)
        
        # TARGET 2: The Weights Tracker (Replacing SQLite dependency)
        # Shape: (N, 1) to allow direct broadcasting later
        user_weights_gpu = torch.zeros((num_users, 1), device=device, dtype=torch.float32)
        
        # Create fast lookup dictionaries
        user_map = dict(zip(df_user['user_id'].astype(str), df_user['user_idx']))
        item_map = dict(zip(df_item['book_id'].astype(str), df_item['item_idx']))
        
        del df_user, df_item
        gc.collect()

        # 3. Stream Interactions and Compute on the fly
        print(f"Streaming interactions from {interactions_path}...")
        ext = os.path.splitext(interactions_path)[1].lower()
        
        if ext == '.parquet':
            import pyarrow.parquet as pq
            parquet_file = pq.ParquetFile(interactions_path)
            batches = parquet_file.iter_batches(batch_size=chunk_size)
            chunk_iterator = (batch.to_pandas() for batch in batches)
        else:
            chunk_iterator = pd.read_csv(interactions_path, chunksize=chunk_size)
            
        processed_rows = 0
        
        for chunk in chunk_iterator:
            chunk['user_id'] = chunk['user_id'].astype(str)
            chunk['book_id'] = chunk['book_id'].astype(str)
            
            chunk['user_idx'] = chunk['user_id'].map(user_map)
            chunk['item_idx'] = chunk['book_id'].map(item_map)
            chunk.dropna(subset=['user_idx', 'item_idx'], inplace=True)
            
            if chunk.empty:
                continue
                
            # CPU Math: Calculate Weights (Strictly Positive Scheme - V2)
            ratings = chunk['rating'].fillna(0).values
            
            # --- Weighted 1 (V1) -
            weights = np.where(
                (ratings >= 1) & (ratings <= 5),
                ratings - 3.0, 
                0.5            
            ).astype(np.float32)
            abs_weights = np.abs(weights)
            # --------------------------------------------
            
            # #---Weighted 2 (V2) ----------------------------------------------------------------
            # conditions = [
            #     ratings >= 4.5,                      # 5 sao
            #     (ratings >= 3.5) & (ratings < 4.5),  # 4 sao
            #     (ratings >= 2.5) & (ratings < 3.5),  # 3 sao
            #     (ratings >= 0.5) & (ratings < 2.5)   # 1-2 sao
            # ]
            # choices = [1.0, 0.8, 0.5, 0.0]
            # # Default is 0.2 for Implicit/Missing (rating < 0.5 or NaN)
            # weights = np.select(conditions, choices, default=0.2).astype(np.float32)
            
            # # Absolute weights are exactly the weights since they are all positive
            # abs_weights = weights
            # #------------------------------------------------------------------------
            
            # Transfer to GPU
            u_idx = torch.tensor(chunk['user_idx'].values, device=device, dtype=torch.long)
            i_idx = torch.tensor(chunk['item_idx'].values, device=device, dtype=torch.long)
            
            w_tensor = torch.tensor(weights, device=device, dtype=torch.float32).unsqueeze(1)
            w_abs_tensor = torch.tensor(abs_weights, device=device, dtype=torch.float32).unsqueeze(1)
            
            # GPU Math 1: Scatter Add Vector * Weight
            v_weighted = item_matrix_gpu[i_idx] * w_tensor
            user_matrix_gpu.index_add_(0, u_idx, v_weighted)
            
            # GPU Math 2: Scatter Add Absolute Weight (Crucial for independence)
            user_weights_gpu.index_add_(0, u_idx, w_abs_tensor)
            
            processed_rows += len(chunk)
            print(f" -> Processed & Accumulated: {processed_rows:,} interactions", end='\r')
            
        print("\nStreaming complete. Executing final normalization phase...")
        
        # 4. Final Normalization using RAM/VRAM tracked weights
        # Prevent division by zero for users with no valid interactions
        user_weights_gpu = torch.clamp(user_weights_gpu, min=1e-9)
        
        # Divide Matrix by Total Absolute Weights
        user_matrix_gpu.div_(user_weights_gpu)
        
        # L2 Normalization
        user_matrix_gpu = F.normalize(user_matrix_gpu, p=2, dim=1)
        
        # 5. Export to CPU and Disk
        print("Transferring Normalized Matrix to CPU and saving...")
        os.makedirs(os.path.dirname(os.path.abspath(output_matrix_path)), exist_ok=True)
        
        user_matrix_cpu = user_matrix_gpu.cpu().numpy()
        np.save(output_matrix_path, user_matrix_cpu)
        
        print(f"[Success] Standalone GPU Builder finished. Matrix saved to {output_matrix_path}")
    @staticmethod
    def check_weight():
        print(f"weight_1")


    