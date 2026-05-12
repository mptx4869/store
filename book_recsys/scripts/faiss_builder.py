import os
import faiss
import numpy as np

class FaissBuilder:
  
    @staticmethod
    def build_ivf_inner_product_index(
        matrix_path: str, 
        output_index_path: str, 
        nlist_multiplier: int = 4
    ):
       
        if not os.path.exists(matrix_path):
            raise FileNotFoundError(f"Matrix file not found at: {matrix_path}")

        print(f"Loading matrix from {matrix_path}...")
        # FAISS strictly requires float32 precision
        matrix = np.load(matrix_path).astype('float32')
        num_items, vector_dim = matrix.shape
        
        # 1. Dynamic nlist calculation (Heuristic: multiplier * sqrt(N))
        nlist = int(nlist_multiplier * np.sqrt(num_items))
        print(f"Matrix loaded. Shape: ({num_items:,}, {vector_dim}). Calculated nlist: {nlist}")
        
        # 2. Initialize the Base Quantizer (Inner Product)
        print("Initializing IndexFlatIP as the base quantizer...")
        quantizer = faiss.IndexFlatIP(vector_dim)
        
        # 3. Initialize the IVF Structure
        print("Initializing IndexIVFFlat structure...")
        index_ivf = faiss.IndexIVFFlat(
            quantizer, 
            vector_dim, 
            nlist, 
            faiss.METRIC_INNER_PRODUCT
        )
        
        # 4. Mandatory Training Phase for IVF
        print("Training the K-Means clustering (This may take a moment)...")
        index_ivf.train(matrix)
        
        if not index_ivf.is_trained:
            raise RuntimeError("CRITICAL ERROR: FAISS Index failed to train.")
            
        # 5. Populate the Index
        print("Adding vectors to the trained index...")
        index_ivf.add(matrix)
        
        # 6. Save to Disk
        print(f"Saving the finalized index to {output_index_path}...")
        os.makedirs(os.path.dirname(os.path.abspath(output_index_path)), exist_ok=True)
        faiss.write_index(index_ivf, output_index_path)
        
        print(f"[Success] IVF Index successfully built and saved.")


# ==========================================
# CÁCH SỬ DỤNG TRONG PIPELINE CỦA BẠN:
# ==========================================
if __name__ == "__main__":
    # Giả sử mô hình ALS vừa chạy xong và nhả ra ma trận Item
    ITEM_MATRIX_PATH = "output/als_item_vectors.npy"
    OUTPUT_INDEX_PATH = "output/als_item_ivf.index"
    # FaissBuilder.build_ivf_inner_product_index(ITEM_MATRIX_PATH, OUTPUT_INDEX_PATH)