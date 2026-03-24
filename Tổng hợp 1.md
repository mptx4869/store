# Tổng hợp và Đánh giá Đồ án Tốt nghiệp Web Bán Sách

Dự án được chia làm hai phần rõ rệt: Backend (trong thư mục `BackEnd`) và Frontend (trong thư mục `store_front`). Dưới đây là phân tích chi tiết về các công nghệ, framework, thuật toán và đánh giá tổng quan, kèm theo các đề xuất nâng cấp sâu hơn.

## 1. Công nghệ & Framework sử dụng

### 1.1. Backend (`BackEnd`)
- **Ngôn ngữ**: Java 21
- **Core Framework**: Spring Boot 3.5.6 (Mới và ổn định)
- **Module của Spring**: Spring Web, Spring Data JPA, Spring Security, Spring Validation.
- **Cơ sở dữ liệu**: PostgreSQL (DB chính), H2 Database (Phục vụ Test / Development).
- **Quản lý CSDL (Migration)**: Flyway Database Migration.
- **Bảo mật & Xác thực**: JSON Web Token (JJWT 0.11.5).
- **Đảm bảo chất lượng phần mềm**: JUnit 5 phục vụ Unit Test (`AdminBookControllerTest`, `OrderControllerTest`,...), Spring `@ControllerAdvice` (`GlobalExceptionHandler.java`) xử lý lỗi đồng bộ.
- **Công cụ hỗ trợ**: Lombok (Giảm thiểu code boilerplate), Gradle (Quản lý dự án).

### 1.2. Frontend (`store_front`)
- **Core Framework / Thư viện**: React 19
- **Build Tool**: Vite cho tốc độ build và phát triển cực nhanh.
- **State Management**: Redux Toolkit & React-Redux.
- **Routing**: React Router DOM v7.
- **Styling**: Tailwind CSS, PostCSS, Autoprefixer.
- **Giao tiếp API**: Axios.
- **Icon**: Lucide-React.

## 2. Các Thuật toán & Logic Xử lý Cốt lõi

1. **Thuật toán Khóa Bi Quan (Pessimistic Locking)**: Áp dụng trong module quản lý hàng tồn kho (`InventoryService.reserveStock`). Khóa Row-level trên Database để ngăn **Race Condition** (vấn đề muôn thuở khi nhiều user mua chung 1 mặt hàng cuối cùng).
2. **Luồng máy trạng thái (State Machine Validation)**: Đảm bảo vòng đời của Order đi đúng logic: `PLACED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED` (Chi tiết tại `OrderService`).
3. **Data Snapshotting (Chụp nhanh dữ liệu)**: Giữ tính toàn vẹn của lịch sử mức giá bằng cách copy `unit_price` khi chuyển từ Giỏ hàng sang Đơn hàng, ngăn chặn việc update giá sách ở Catalog làm sai lệch doanh thu quá khứ.
4. **Data Normalization & Variant Strategy**: Áp dụng chuẩn hóa tách biệt dữ liệu gốc `Books` đa hình bằng `Product_Skus` tránh dư thừa.

---

## 3. Đánh giá Tổng quan Hệ thống

### **Điểm mạnh**
- **Tech Stack hiện đại & Chuẩn chỉnh**: Khai thác sức mạnh của Java 21, React 19 và TailwindCSS. Project có cấu trúc phân tầng Clean Architecture hiển nhiên, áp dụng tốt các design pattern cơ bản (DTO, Repository).
- **Domain-Driven Design (Tư duy nghiệp vụ tốt)**: Data Model thể hiện sự am hiểu E-commerce (chia User vs UserProfile, Book vs SKU, xử lý Snapshot Giá rổ).
- **Đảm bảo độ tin cậy của Code**: Việc có thư mục `Test` riêng biệt xử lý Unit Testing Controller và sử dụng ControllerAdvice gom các Exception (`ConflictException`, `InsufficientStockException`) là tư duy của Senior Engineer/Môi trường doanh nghiệp.

### **Điểm Cần Cải Thiện Ở Code và Các Tính năng Hiện Có**
- **Bottlenecks khi Checkout (Do Pessimistic Locking)**: Việc lock database quá gắt ở `reserveStock` hiệu quả tuyệt đối về mặt chính xác nhưng sẽ đánh đổi bằng hiệu năng (Performance). Nếu lượng traffic lớn, database sẽ quá tải. 
  - *Giải pháp*: Chuyển sang dùng **Optimistic Locking** (chạy `@Version` column trong Entity) hoặc đẩy luồng đặt hàng vào **Message Queue** (như Kafka / RabbitMQ hoặc Redis Queue) để xử lý bất đồng bộ.
- **Thiếu Caching Cơ Sở Dữ Liệu**: `BookService.java` hiện đọc trực tiếp từ Database ở mỗi request truy vấn danh sách sách. 
  - *Giải pháp*: Tích hợp **Redis Cache** (Spring Cache) cho các trang có độ view cao như `getAllBooks`, `getNewBooks` để giảm tải Queries vào PostgreSQL.
- **Nâng cấp Hệ Thống Tì Kiếm Text (Search)**: Sử dụng các query Data JPA truyền thống (`LIKE %keyword%`) sẽ bị chậm và không bắt được lỗi chính tả (typo).
  - *Giải pháp*: Tích hợp Full-Text Search hoặc **ElasticSearch**.

### **Các Tính Năng Mới Cần Thiết (Đề Xuất Phát Triển Thêm)**
Bên cạnh code nền tảng tốt, để đây trở thành một phần mềm thực chiến toàn diện, dự án nên triển khai thêm các module sau:

1. **Cổng Thanh Toán Điện Tử (Payment Gateway Gateway)**:
   - *Tình trạng hiện tại*: Chưa tìm thấy logic thanh toán (mới chỉ lưu order).
   - *Yêu cầu*: Phải tích hợp cổng thanh toán Sandbox (Ví dụ: **VNPAY, MoMo Payment, ZaloPay** hoặc Stripe đối với thị trường quốc tế) để xử lý giao dịch thực tế thay vì COD (Cash on Delivery).
2. **Hệ Thống Đánh Giá & Bình Luận Sản Phẩm (Rating / Reviews)**:
   - Thương mại điện tử không thể thiếu "Social Proof" (Bằng chứng xã hội). Việc cho phép user để lại số sao (1-5) và bình luận sẽ tăng độ uy tín. Đòi hỏi Database phải có bảng `reviews`.
3. **Thuật Toán AI Thay Thế Hiển Thị Đơn Điệu (Recommendation Engine)**:
   - Bổ sung hệ gợi ý sách theo hành vi (Collaborative Filtering / TF-IDF Content-based) ở cuối trang chi tiết sách (`BookDetailPage.jsx` hiển thị: "Có thể bạn cũng muốn mua...").
4. **Hệ Thống Gửi Email Tự Động (Email Notifications)**:
   - Tích hợp `JavaMailSender` (với SMTP của Gmail hoặc SendGrid) để tự động gửi thông báo hóa đơn Điện Tử hoặc thư "Welcome" khi người dùng đăng ký Account mới.
5. **Trang Quản trị Thống Kê Nâng Cao (Admin Dashboard Analytics)**:
   - Hiện đã có `Admin...Controller`, tuy nhiên nên có các Endpoint dạng báo cáo: Doanh số theo ngày, Top 5 cuốn sách bán chạy nhất, Tỉ lệ Abandoned Cart (rời bỏ giỏ hàng) để vẽ biểu đồ line chart / bar chart trên Frontend.
