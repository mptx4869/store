import psycopg2
import requests
from bs4 import BeautifulSoup
from tqdm import tqdm
import time

def get_db_connection():
    return psycopg2.connect(
        host="172.25.47.154",
        port="5432",
        dbname="thesis",
        user="nhanhoa",
        password="123"
    )

def fetch_image_from_goodreads(book_id):
    url = f"https://www.goodreads.com/book/show/{book_id}"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            # Lấy src từ element có class ResponsiveImage
            img = soup.find('img', class_='ResponsiveImage')
            if img and img.has_attr('src'):
                return img['src']
    except Exception as e:
        pass
    
    return None

def main():
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            # Tìm những quyển sách có image_url là null hoặc rỗng
            cur.execute("SELECT id FROM books WHERE image_url IS NULL OR image_url = '';")
            books = cur.fetchall()
            
        if not books:
            print("Không tìm thấy sách nào thiếu hình ảnh.")
            return

        print(f"Tìm thấy {len(books)} sách cần cập nhật hình ảnh.")
        
        # Thanh tiến trình
        for row in tqdm(books, desc="Đang cập nhật hình ảnh", unit="sách"):
            book_id = row[0]
            
            image_url = fetch_image_from_goodreads(book_id)
            if image_url:
                with conn.cursor() as cur:
                    cur.execute("UPDATE books SET image_url = %s WHERE id = %s", (image_url, book_id))
                conn.commit()
                
            # Tránh bị chặn bởi Goodreads do spam request liên tục
            time.sleep(1)
            
    except Exception as e:
        print(f"Có lỗi xảy ra: {e}")
    finally:
        conn.close()
        print("Đã hoàn tất!")

if __name__ == "__main__":
    main()
