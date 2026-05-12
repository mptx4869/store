import duckdb
import queue
import numpy as np
class BookProcessor:
    def __init__(self, item_index_path: str):
        print(f"Loading Book mapping from {item_index_path} ...")
        
        query = f"SELECT CAST(book_id AS VARCHAR), CAST(COALESCE(row_idx, book_id) AS INTEGER) FROM read_csv_auto('{item_index_path}')"
        
        with duckdb.connect(':memory:') as conn:
            results = conn.execute(query).fetchall()
        
        self.id_to_idx = {str(row[0]): int(row[1]) for row in results}
        self.idx_to_id = {int(row[1]): str(row[0]) for row in results}
        
        self.total_books = len(self.id_to_idx)
        print(f" -> Loaded {self.total_books:,} books into RAM dictionary.")

    def get_idx(self, book_id: str) -> int:
        return self.id_to_idx.get(str(book_id), -1)

    def get_id(self, item_idx: int) -> str:
        return self.idx_to_id.get(item_idx, None)

class UserProcessor:
    _POOL_SIZE = 4  # Số kết nối DuckDB song song tối đa

    def __init__(self, user_index_path: str, history_db_path: str):
        print(f"Loading User mapping from {user_index_path}...")

        query = f"SELECT CAST(user_id AS VARCHAR), CAST(user_idx AS INTEGER) FROM read_csv_auto('{user_index_path}')"
        with duckdb.connect(':memory:') as mem_conn:
            results = mem_conn.execute(query).fetchall()

        self.id_to_idx = {str(row[0]): int(row[1]) for row in results}
        self.idx_to_id = {int(row[1]): str(row[0]) for row in results}
        self.total_users = len(self.id_to_idx)
        print(f" -> Loaded {self.total_users:,} users into RAM dictionary.")

        # Connection pool: 
        print(f"Building DuckDB connection pool (size={self._POOL_SIZE})...")
        self._pool = queue.Queue()
        for _ in range(self._POOL_SIZE):
            self._pool.put(duckdb.connect(history_db_path, read_only=True))
        print(f" -> Pool ready ({self._POOL_SIZE} connections to user_history.db)")

    def get_idx(self, user_id: str) -> int:
        return self.id_to_idx.get(str(user_id), -1)

    def get_history_idx(self, user_id: str) -> set:
        """Get user's seen books as set of indices."""
        conn = self._pool.get()          # wait if pool is busy (blocking = safe)
        try:
            result = conn.execute(
                "SELECT seen_books FROM user_history WHERE user_id = ?",
                [str(user_id)]
            ).fetchone()
        finally:
            self._pool.put(conn)         # Return connection to pool whether there is an error or not

        return set(result[0]) if result and result[0] is not None else set()

    def get_history_length(self, user_id: str) -> int:
        """Get total number of books in user's history."""
        conn = self._pool.get()
        try:
            result = conn.execute(
                "SELECT total FROM user_history WHERE user_id = ?",
                [str(user_id)]
            ).fetchone()
        finally:
            self._pool.put(conn)

        return int(result[0]) if result and result[0] is not None else 0