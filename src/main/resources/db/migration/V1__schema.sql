-- Schema for a bookstore database based on the provided PostgreSQL dump

-- Function update at updated_at 
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Users Table 
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    username VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP
);

-- Trigger apply for users
CREATE TRIGGER set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Roles Table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Categories Table
CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

-- Books Table
CREATE TABLE books (
    book_id BIGSERIAL PRIMARY KEY,
    author VARCHAR(255),
    cover_image VARCHAR(255),
    description TEXT,
    isbn VARCHAR(255),
    price NUMERIC(38,2),
    published_date DATE,
    publisher VARCHAR(255),
    stock INTEGER,
    title VARCHAR(255) NOT NULL
);

-- Book_Categories Table (many-to-many relation between books and categories)
CREATE TABLE book_categories (
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP(6),
    priority INTEGER,
    PRIMARY KEY (book_id, category_id),
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- Orders Table
CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6),
    payment_method VARCHAR(255),
    shipping_address VARCHAR(255),
    status VARCHAR(255),
    total_amount NUMERIC(38,2),
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Order_Items Table
CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    price_at_purchase NUMERIC(38,2),
    quantity INTEGER,
    book_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);