# import os
# import gc
# import csv
# import pyarrow.parquet as pq
# import pandas as pd
# import numpy as np
# import psutil
# import torch
# from sentence_transformers import SentenceTransformer
# from typing import Optional

# class ItemVectorBuilder:
#     NOISE_SHELVES = {
#         'to-read', 'currently-reading', 'owned', 'books-i-own',
#         'favourites', 'favorite', 'default', 'to-buy', 'maybe',
#         'wish-list', 'library', 'ebook', 'kindle'
#     }

#     def __init__(self, 
#                  input_parquet: str, 
#                  output_matrix_path: str, 
#                  output_index_path: str, 
#                  model_name: str, 
#                  chunk_size: int = 5000, 
#                  encode_batch_size: int = 256, 
#                  max_desc_chars: int = 300,
#                  device: Optional[str] = None):
        
#         self.input_parquet = input_parquet
#         self.output_matrix_path = output_matrix_path
#         self.output_index_path = output_index_path
#         self.model_name = model_name
#         self.chunk_size = chunk_size
#         self.encode_batch_size = encode_batch_size
#         self.max_desc_chars = max_desc_chars
        
#         # Determine the file extension
#         _, ext = os.path.splitext(self.output_matrix_path)
#         self.ext = ext.lower()
#         if self.ext not in ['.npy', '.memmap']:
#             raise ValueError("output_matrix_path must end with .npy or .memmap")
            
#         # Determine the execution device (GPU preferred)
#         if device is None:
#             self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
#         else:
#             self.device = device
            
#         self.model = None
#         self.storage = None

#     def _init_model(self) -> int:
#         print(f"Loading SentenceTransformer model '{self.model_name}' on device: {self.device}...")
#         self.model = SentenceTransformer(self.model_name, device=self.device)
#         return self.model.get_sentence_embedding_dimension()

#     def _apply_noise_filter(self, text: str) -> str:
#         if not isinstance(text, str) or not text.strip():
#             return ""
        
#         items = [item.strip() for item in text.split(',')]
#         filtered_items = [
#             item for item in items 
#             if item.lower() not in self.NOISE_SHELVES
#         ]
#         return ", ".join(filtered_items)

#     def _build_natural_text(self, row: pd.Series) -> str:
#         """Combines row columns into natural BERT-friendly text structure."""
#         title = str(row.get('title', '')).strip()
#         author = str(row.get('author', '')).strip()
        
#         # Process and filter noise shelves from the genres
#         raw_genres = str(row.get('genres', '')).strip()
#         genres = self._apply_noise_filter(raw_genres)
        
#         desc = str(row.get('description', '')).strip()
#         if len(desc) > self.max_desc_chars:
#             desc = desc[:self.max_desc_chars]
        
#         parts = []
#         if title:
#             parts.append(f"Title: {title}")
#         if author:
#             parts.append(f"Author: {author}")
#         if genres:
#             parts.append(f"Genres: {genres}")
#         if desc:
#             parts.append(f"Description: {desc}")
            
#         return " ".join(parts)

#     def _initialize_storage(self, num_rows: int, vector_dim: int):
#         """Initializes storage backend for matrix based on the file extension."""
#         os.makedirs(os.path.dirname(os.path.abspath(self.output_matrix_path)), exist_ok=True)
#         if self.ext == '.memmap':
#             print(f"Initializing raw memmap target: shape={num_rows}x{vector_dim}")
#             self.storage = np.memmap(
#                 self.output_matrix_path, 
#                 dtype=np.float32, 
#                 mode='w+', 
#                 shape=(num_rows, vector_dim)
#             )
#         elif self.ext == '.npy':
#             print(f"Initializing NPY memmap target: shape={num_rows}x{vector_dim}")
#             self.storage = np.lib.format.open_memmap(
#                 self.output_matrix_path,
#                 mode='w+',
#                 dtype=np.float32,
#                 shape=(num_rows, vector_dim)
#             )

#     def _write_chunk_vectors(self, vectors: np.ndarray, start_idx: int):
#         """Dispatches chunk array to storage backend."""
#         end_idx = start_idx + len(vectors)
#         self.storage[start_idx:end_idx] = vectors
#         self.storage.flush()

#     def _get_ram_usage(self) -> float:
#         """Returns the current psutil memory usage in MB."""
#         process = psutil.Process()
#         return process.memory_info().rss / (1024 ** 2)

#     def build_index_csv(self):
#         """Standalone method to build or rebuild the index.csv mapping from the source parquet."""
#         print(f"Building index CSV directly from {self.input_parquet}...")
        
#         # Read only the 'book_id' column to save memory
#         df_ids = pd.read_parquet(self.input_parquet, columns=['book_id'])
        
#         # Create row_idx based on the order
#         df_ids['row_idx'] = np.arange(len(df_ids))
        
#         # Reorder columns to match ['row_idx', 'book_id']
#         df_ids = df_ids[['row_idx', 'book_id']]
        
#         # Check order validity with original
#         original_num_rows = pq.ParquetFile(self.input_parquet).metadata.num_rows
#         if len(df_ids) != original_num_rows:
#             raise RuntimeError(f"Index size mismatch: Parquet has {original_num_rows} rows but index generated {len(df_ids)} rows.")
            
#         # Save to output_index_path
#         os.makedirs(os.path.dirname(os.path.abspath(self.output_index_path)), exist_ok=True)
#         df_ids.to_csv(self.output_index_path, index=False)
        
#         print(f"Successfully generated {self.output_index_path} with {len(df_ids)} records.")
#         return df_ids

#     def build(self):
#         """Main execution pipeline."""
#         print(f"Starting vector build from {self.input_parquet}")
#         os.makedirs(os.path.dirname(os.path.abspath(self.output_index_path)), exist_ok=True)
        
#         # 1. Total rows measurement via parquet metadata
#         pf = pq.ParquetFile(self.input_parquet)
#         num_rows = pf.metadata.num_rows
#         print(f"Total rows in dataset: {num_rows}")
        
#         if num_rows == 0:
#             raise ValueError("Input parquet file is empty.")
            
#         # 2. Setup Vector model and Storage
#         vector_dim = self._init_model()
#         self._initialize_storage(num_rows, vector_dim)
        
#         # 3. Loop through parquet file iter_batches
#         current_idx = 0
#         peak_ram = self._get_ram_usage()
        
#         for batch in pf.iter_batches(batch_size=self.chunk_size):
#             df_batch = batch.to_pandas()
            
#             if df_batch.empty:
#                 continue
                
#             # 4. Filter text columns to natural BERT text format
#             texts = df_batch.apply(self._build_natural_text, axis=1).tolist()
            
#             # 5. Pass into model to encode text -> vectors
#             vectors = self.model.encode(
#                 texts,
#                 batch_size=self.encode_batch_size,
#                 show_progress_bar=True,
#                 normalize_embeddings=True
#             )
            
#             # 6. Apply to specific backend target layer
#             self._write_chunk_vectors(vectors, current_idx)
            
#             current_idx += len(vectors)
            
#             # 7. Collect Garbage and update Memory Logging Checks
#             del texts, vectors, df_batch, batch
#             gc.collect()
            
#             current_ram = self._get_ram_usage()
#             peak_ram = max(peak_ram, current_ram)
#             print(f"Processed chunks: {current_idx}/{num_rows} - Current RAM: {current_ram:.1f} MB")
                
#         # 8. Unload chunks and flush
#         if hasattr(self.storage, 'flush'):
#             self.storage.flush()
#         del self.storage
#         gc.collect()
        
#         # 9. Build the index.csv map safely at the end
#         self.build_index_csv()
            
#         print("====== BUILD SUMMARY ======")
#         print(f"Total Rows Processed: {current_idx}")
#         print(f"Output Matrix path: {self.output_matrix_path}")
#         print(f"Output Index path: {self.output_index_path}")
#         print(f"Peak Runtime RAM Usage: {peak_ram:.1f} MB")
#         print("===========================")

import os
import gc
import csv
import pyarrow.parquet as pq
import pandas as pd
import numpy as np
import psutil
import torch
from sentence_transformers import SentenceTransformer
from typing import Optional

class ItemVectorBuilder:
    NOISE_SHELVES = {
        'to-read', 'currently-reading', 'owned', 'books-i-own',
        'favourites', 'favorite', 'default', 'to-buy', 'maybe',
        'wish-list', 'library', 'ebook', 'kindle'
    }

    def __init__(self, 
                 input_parquet: str, 
                 output_matrix_path: str, 
                 output_index_path: str, 
                 model_name: str, 
                 chunk_size: int = 5000, 
                 encode_batch_size: int = 256, 
                 max_desc_chars: int = 300,
                 device: Optional[str] = None):
        
        self.input_parquet = input_parquet
        self.output_matrix_path = output_matrix_path
        self.output_index_path = output_index_path
        self.model_name = model_name
        self.chunk_size = chunk_size
        self.encode_batch_size = encode_batch_size
        self.max_desc_chars = max_desc_chars
        
        # Determine the file extension
        _, ext = os.path.splitext(self.output_matrix_path)
        self.ext = ext.lower()
        if self.ext not in ['.npy', '.memmap']:
            raise ValueError("output_matrix_path must end with .npy or .memmap")
            
        # Determine the execution device (GPU preferred)
        if device is None:
            self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        else:
            self.device = device
            
        self.model = None
        self.storage = None

    def _init_model(self) -> int:
        print(f"Loading SentenceTransformer model '{self.model_name}' on device: {self.device}...")
        self.model = SentenceTransformer(self.model_name, device=self.device)
        return self.model.get_sentence_embedding_dimension()

    def _apply_noise_filter(self, text: str) -> str:
        if not isinstance(text, str) or not text.strip():
            return ""
        
        items = [item.strip() for item in text.split(',')]
        filtered_items = [
            item for item in items 
            if item.lower() not in self.NOISE_SHELVES
        ]
        return ", ".join(filtered_items)

    def _build_natural_text(self, row: pd.Series) -> str:
        """Combines row columns into natural BERT-friendly text structure."""
        title = str(row.get('title', '')).strip()
        author = str(row.get('author', '')).strip()
        
        # Process and filter noise shelves from the genres
        raw_genres = str(row.get('genres', '')).strip()
        genres = self._apply_noise_filter(raw_genres)
        
        desc = str(row.get('description', '')).strip()
        if len(desc) > self.max_desc_chars:
            desc = desc[:self.max_desc_chars]
        
        parts = []
        if title:
            parts.append(f"Title: {title}")
        if author:
            parts.append(f"Author: {author}")
        if genres:
            parts.append(f"Genres: {genres}")
        if desc:
            parts.append(f"Description: {desc}")
            
        return " ".join(parts)

    def _initialize_storage(self, num_rows: int, vector_dim: int):
        """Initializes storage backend for matrix based on the file extension."""
        os.makedirs(os.path.dirname(os.path.abspath(self.output_matrix_path)), exist_ok=True)
        if self.ext == '.memmap':
            print(f"Initializing raw memmap target: shape={num_rows}x{vector_dim}")
            self.storage = np.memmap(
                self.output_matrix_path, 
                dtype=np.float32, 
                mode='w+', 
                shape=(num_rows, vector_dim)
            )
        elif self.ext == '.npy':
            print(f"Initializing NPY memmap target: shape={num_rows}x{vector_dim}")
            self.storage = np.lib.format.open_memmap(
                self.output_matrix_path,
                mode='w+',
                dtype=np.float32,
                shape=(num_rows, vector_dim)
            )

    def _write_chunk_vectors(self, vectors: np.ndarray, start_idx: int):
        """Dispatches chunk array to storage backend."""
        end_idx = start_idx + len(vectors)
        self.storage[start_idx:end_idx] = vectors
        self.storage.flush()

    def _get_ram_usage(self) -> float:
        """Returns the current psutil memory usage in MB."""
        process = psutil.Process()
        return process.memory_info().rss / (1024 ** 2)

    def build_index_csv(self):
        """Standalone method to build or rebuild the index.csv mapping from the source parquet."""
        print(f"Building index CSV directly from {self.input_parquet}...")
        
        # Read only the 'book_id' column to save memory
        df_ids = pd.read_parquet(self.input_parquet, columns=['book_id'])
        
        # Create row_idx based on the order
        df_ids['row_idx'] = np.arange(len(df_ids))
        
        # Reorder columns to match ['row_idx', 'book_id']
        df_ids = df_ids[['row_idx', 'book_id']]
        
        # Check order validity with original
        original_num_rows = pq.ParquetFile(self.input_parquet).metadata.num_rows
        if len(df_ids) != original_num_rows:
            raise RuntimeError(f"Index size mismatch: Parquet has {original_num_rows} rows but index generated {len(df_ids)} rows.")
            
        # Save to output_index_path
        os.makedirs(os.path.dirname(os.path.abspath(self.output_index_path)), exist_ok=True)
        df_ids.to_csv(self.output_index_path, index=False)
        
        print(f"Successfully generated {self.output_index_path} with {len(df_ids)} records.")
        return df_ids

    def build(self):
        """Main execution pipeline.
        
        Key design: index.csv is built IN SYNC with the vector encoding loop.
        Each batch writes vectors to the matrix AND appends book_ids to the
        index file in the same iteration, guaranteeing row_idx alignment.
        """
        print(f"Starting vector build from {self.input_parquet}")
        os.makedirs(os.path.dirname(os.path.abspath(self.output_index_path)), exist_ok=True)
        
        # 1. Total rows measurement via parquet metadata
        pf = pq.ParquetFile(self.input_parquet)
        num_rows = pf.metadata.num_rows
        print(f"Total rows in dataset: {num_rows}")
        
        if num_rows == 0:
            raise ValueError("Input parquet file is empty.")
            
        # 2. Setup Vector model and Storage
        vector_dim = self._init_model()
        self._initialize_storage(num_rows, vector_dim)
        
        # 3. Open index CSV writer alongside the encoding loop
        #    so that book_id <-> row_idx mapping is built in the
        #    exact same order as vectors are written.
        current_idx = 0
        peak_ram = self._get_ram_usage()
        
        with open(self.output_index_path, 'w', newline='') as idx_file:
            writer = csv.writer(idx_file)
            writer.writerow(['row_idx', 'book_id'])  # header
        
            for batch in pf.iter_batches(batch_size=self.chunk_size):
                df_batch = batch.to_pandas()
                
                if df_batch.empty:
                    continue
                
                # ---- Write index rows for this batch (same order) ----
                batch_book_ids = df_batch['book_id'].tolist()
                for i, book_id in enumerate(batch_book_ids):
                    writer.writerow([current_idx + i, book_id])
                idx_file.flush()
                    
                # ---- Encode text -> vectors ----
                texts = df_batch.apply(self._build_natural_text, axis=1).tolist()
                
                vectors = self.model.encode(
                    texts,
                    batch_size=self.encode_batch_size,
                    show_progress_bar=True,
                    normalize_embeddings=True
                )
                
                # ---- Write vectors to matrix at the same position ----
                self._write_chunk_vectors(vectors, current_idx)
                
                current_idx += len(vectors)
                
                # ---- Cleanup ----
                del texts, vectors, df_batch, batch, batch_book_ids
                gc.collect()
                
                current_ram = self._get_ram_usage()
                peak_ram = max(peak_ram, current_ram)
                print(f"Processed chunks: {current_idx}/{num_rows} - Current RAM: {current_ram:.1f} MB")
                
        # 4. Unload storage and flush
        if hasattr(self.storage, 'flush'):
            self.storage.flush()
        del self.storage
        gc.collect()
        
        # 5. Verify row count consistency
        index_df = pd.read_csv(self.output_index_path)
        if len(index_df) != current_idx:
            raise RuntimeError(
                f"CRITICAL: Index has {len(index_df)} rows but matrix has {current_idx} rows. "
                f"Ordering is NOT guaranteed."
            )
        if current_idx != num_rows:
            raise RuntimeError(
                f"CRITICAL: Processed {current_idx} rows but parquet has {num_rows} rows."
            )
            
        print("====== BUILD SUMMARY ======")
        print(f"Total Rows Processed: {current_idx}")
        print(f"Output Matrix path: {self.output_matrix_path}")
        print(f"Output Index path: {self.output_index_path}")
        print(f"Peak Runtime RAM Usage: {peak_ram:.1f} MB")
        print(f"Index-Matrix alignment: VERIFIED")
        print("===========================")