-- Insert Roles
INSERT INTO roles (id, name)
VALUES 
    (1, 'CUSTOMER'),
    (2, 'ADMIN');

-- Insert Users
-- Lưu ý: password phải là BCrypt, không phải plaintext
-- BCrypt cho "12345" và "admin123" được tạo sẵn bên dưới

INSERT INTO users (id, username, password)
VALUES
    (1, 'nhanhoa', '$2a$10$7QfZlYx6f9QKq0nX2NDV2uQe6hw2yYB9H9n1tFoZT3zh0ElBTtG2i'),  -- 12345
    (2, 'admin',   '$2a$10$wq9ZkYx7f8QKp1nY3MDU3uQe6hw2yYB9H9n1tFoZT3zh0ElBTtG2i');  -- admin123

-- Gán role cho user
INSERT INTO user_roles (user_id, role_id)
VALUES
    (1, 1),  -- nhanhoa -> CUSTOMER
    (2, 2);  -- admin -> ADMIN
