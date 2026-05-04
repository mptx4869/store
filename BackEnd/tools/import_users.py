import psycopg2
import psycopg2.extras
import pandas as pd
import bcrypt
import os
import argparse
from tqdm import tqdm

def parse_args():
    parser = argparse.ArgumentParser(description="Import users from CSV into Postgres")
    parser.add_argument("--csv", default=r"D:\WorkSpace\store\BackEnd\src\main\resources\data\test_main.csv", help="Path to csv file")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "172.25.47.154"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "5432")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "thesis"))
    parser.add_argument("--db-user", default=os.getenv("DB_USER", "nhanhoa"))
    parser.add_argument("--db-password", default=os.getenv("DB_PASSWORD", "123"))
    return parser.parse_args()

def hash_password(password: str) -> str:
    """
    Sử dụng bcrypt để mã hóa password, tương thích với BCryptPasswordEncoder của Spring Security.
    """
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def get_or_create_role(cur, role_name: str) -> int:
    cur.execute("SELECT id FROM roles WHERE name = %s", (role_name,))
    row = cur.fetchone()
    if row:
        return row[0]
    
    cur.execute("INSERT INTO roles (name, description) VALUES (%s, %s) RETURNING id", (role_name, f"{role_name} role"))
    return cur.fetchone()[0]

def ensure_user(cur, username: str, email: str, password: str, role_id: int):
    # Hash password cho các tài khoản admin riêng biệt
    hashed = hash_password(password)
    cur.execute(
        """
        INSERT INTO users (username, email, password_hash, role_id, status)
        VALUES (%s, %s, %s, %s, 'ACTIVE')
        ON CONFLICT (username) DO NOTHING
        """,
        (username, email, hashed, role_id)
    )

def main():
    args = parse_args()
    if not os.path.exists(args.csv):
        print(f"Error: Không tìm thấy file CSV tại {args.csv}")
        return

    # Kết nối Database
    conn = psycopg2.connect(
        host=args.db_host,
        port=args.db_port,
        dbname=args.db_name,
        user=args.db_user,
        password=args.db_password,
    )

    try:
        with conn.cursor() as cur:
            print("Đang khởi tạo Roles...")
            super_admin_role_id = get_or_create_role(cur, "SUPER_ADMIN")
            admin_role_id = get_or_create_role(cur, "ADMIN")
            customer_role_id = get_or_create_role(cur, "CUSTOMER")
            
            print("Đang khởi tạo các tài khoản Admin...")
            ensure_user(cur, "superadmin", "superadmin@gmail.com", "superadmin", super_admin_role_id)
            ensure_user(cur, "admin", "admin@gmail.com", "admin123", admin_role_id)
            
            conn.commit()

            print("Đang mã hóa mật khẩu mặc định (123123)...")
            default_password_hash = hash_password("123123")
            
            print("Đang đọc dữ liệu từ file CSV và import theo batch...")
            # Sử dụng chunksize để không load toàn bộ file vào RAM
            chunk_size = 1000
            
            for chunk in tqdm(pd.read_csv(args.csv, chunksize=chunk_size), desc="Đang import batches"):
                if 'user_id' not in chunk.columns:
                    print("Lỗi: Không tìm thấy cột 'user_id' trong file CSV.")
                    return
                
                # Loại bỏ trùng lặp trong từng batch
                unique_users = chunk['user_id'].dropna().astype(str).unique()
                
                # Tạo list tuples cho bulk insert
                values = [
                    (str(user_id), f"{str(user_id)}@gmail.com", default_password_hash, customer_role_id)
                    for user_id in unique_users
                ]
                
                if not values:
                    continue
                
                query = """
                    INSERT INTO users (username, email, password_hash, role_id)
                    VALUES %s
                    ON CONFLICT (username) DO NOTHING
                """
                
                try:
                    psycopg2.extras.execute_values(cur, query, values)
                except Exception as e:
                    conn.rollback()
                    print(f"Lỗi khi import batch: {e}")
                    raise e
                    
            conn.commit()
            print("Đã hoàn tất import theo batch!")

    except Exception as e:
        print(f"Đã xảy ra lỗi: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == "__main__":
    main()
