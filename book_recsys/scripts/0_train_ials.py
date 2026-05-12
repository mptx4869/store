import polars as pl
import numpy as np
import scipy.sparse as sp
from implicit.gpu.als import AlternatingLeastSquares
from implicit.evaluation import precision_at_k, mean_average_precision_at_k
import logging
import os
import gc

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(name)s - %(message)s')
logger = logging.getLogger("iALS_Trainer")

def get_dimensions(user_idx_path, book_idx_path):
    logger.info("Calculating dimensions...")
    num_users = pl.scan_csv(user_idx_path).select(pl.len()).collect().item()
    num_books = pl.scan_csv(book_idx_path).select(pl.len()).collect().item()
    logger.info(f"Dimensions: {num_users} Users, {num_books} Books")
    return num_users, num_books

def load_matrix_streaming(data_path, user_idx_path, book_idx_path, num_users, num_books, alpha=40):
    """
    Use Polars LazyFrames and Streaming engine to perform Out-Of-Core joins and confidence 
    calculations, mitigating RAM overflow on 170M+ row files.
    """
    users_lf = pl.scan_csv(user_idx_path).select(['user_id', 'user_idx'])
    books_lf = pl.scan_csv(book_idx_path).select(['book_id', 'row_idx']).rename({'row_idx': 'book_idx'})
    
    # Lazy DataFrame chain for memory optimization
    data_lf = pl.scan_csv(data_path)
    
    data_lf = data_lf.join(users_lf, on='user_id', how='inner') \
                     .join(books_lf, on='book_id', how='inner')
    
    data_lf = data_lf.with_columns(
        pl.when(pl.col('rating').is_not_null() & (pl.col('rating') >= 1.0) & (pl.col('rating') <= 5.0))
        .then(pl.col('rating'))
        .otherwise(1.0)
        .alias('weight')
    ).with_columns((1.0 + alpha * pl.col('weight')).alias('confidence'))
    
    # Collect only the required integer columns for CSR Matrix construction
    # streaming=True evaluates in chunks to safeguard RAM
    logger.info(f"Executing Streaming pipeline for {data_path}...")
    df = data_lf.select(['user_idx', 'book_idx', 'confidence']).collect(streaming=True)
    
    # Convert efficiently
    logger.info(f"Constructing CSR Matrix for {data_path}...")
    matrix = sp.csr_matrix(
        (df['confidence'].to_numpy(), (df['user_idx'].to_numpy(), df['book_idx'].to_numpy())),
        shape=(num_users, num_books)
    )
    
    # Clear Polars DataFrame directly from memory
    del df
    gc.collect()
    
    return matrix
data_dir="/content/drive/My Drive/book_recsys/data/processed_2"
model_dir=data_dir + "/model_v1"
def train_ials_pipeline(

    train_csv=data_dir + "/train_main.csv",
    valid_csv=data_dir + "/valid_main.csv",
    user_index_csv= model_dir + "/user_index.csv",
    book_index_csv=model_dir + "/cb_sbert_book_index.csv",
    factors=384,
    iterations=20,
    regularization=0.01,
    alpha=40,
    output_dir=model_dir
):
    os.makedirs(output_dir, exist_ok=True)
    
    num_users, num_books = get_dimensions(user_index_csv, book_index_csv)
    
    train_matrix = load_matrix_streaming(train_csv, user_index_csv, book_index_csv, num_users, num_books, alpha)
    valid_matrix = load_matrix_streaming(valid_csv, user_index_csv, book_index_csv, num_users, num_books, alpha)
    
    logger.info("Initializing GPU ALS Model...")
    model = AlternatingLeastSquares(
        factors=factors,
        regularization=regularization,
        iterations=iterations,
        calculate_training_loss=True,
        random_state=42
    )
    
    logger.info("Starting training on GPU...")
    model.fit(train_matrix)
    
    logger.info("Evaluating Model...")
    p_at_k = precision_at_k(model, train_matrix, valid_matrix, K=10)
    map_at_k = mean_average_precision_at_k(model, train_matrix, valid_matrix, K=10)
    logger.info(f"Precision@10: {p_at_k:.4f} | MAP@10: {map_at_k:.4f}")
    
    logger.info("Saving factors...")
    user_factors = model.user_factors.to_numpy() if hasattr(model.user_factors, 'to_numpy') else model.user_factors
    item_factors = model.item_factors.to_numpy() if hasattr(model.item_factors, 'to_numpy') else model.item_factors
    
    np.save(os.path.join(output_dir, "ials_user_factors.npy"), user_factors)
    np.save(os.path.join(output_dir, "ials_book_factors.npy"), item_factors)
    logger.info("Pipeline Execution Completed.")

if __name__ == "__main__":
    train_ials_pipeline()
