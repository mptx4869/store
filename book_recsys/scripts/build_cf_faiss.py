import os
import logging
import numpy as np
import faiss

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

def build_faiss_index(npy_path: str, index_out_path: str):
    logger.info(f"Loading Item Matrix from: {npy_path}")
    if not os.path.exists(npy_path):
        logger.error("File not found! Please run the cf.ipynb notebook first.")
        return

    # Mmap mode ensures low memory footprint
    item_vectors = np.load(npy_path, mmap_mode='r')
    
    # Cast to float32 as FAISS strictly requires it
    # We do copy to avoid memory issues when pushing to FAISS
    vectors_float32 = np.array(item_vectors, dtype=np.float32)
    
    dim = vectors_float32.shape[1]
    ntotal = vectors_float32.shape[0]
    
    logger.info(f"Loaded matrix: {ntotal} items, {dim} dimensions.")
    logger.info("Building FAISS IndexFlatIP (Inner Product) for iALS vectors...")
    
    # IndexFlatIP calculates exact dot products, perfectly matching matrix factorization setup.
    index = faiss.IndexFlatIP(dim)
    index.add(vectors_float32)
    
    logger.info(f"Saving FAISS index to: {index_out_path}")
    faiss.write_index(index, index_out_path)
    logger.info("Done!")

if __name__ == "__main__":
    base_dir = '/content/drive/My Drive/Thesis/book_recsys'
    # Optional fallback for local dev
    if not os.path.exists(base_dir):
        # Adjusting to local test paths if not on colab
        base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    
    out_dir = f"{base_dir}/data/processed/test"
    
    # These must match exactly the outputs from cf.ipynb
    item_vectors_path = f"{out_dir}/cf_als_v1_item_matrix.npy"
    faiss_index_path = f"{out_dir}/cf_als_v1_item.index"
    
    build_faiss_index(item_vectors_path, faiss_index_path)
