-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS carrental;
USE carrental;

-- Clear existing data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE feedbacks;
TRUNCATE TABLE payments;
TRUNCATE TABLE bookings;
TRUNCATE TABLE car_schedules;
TRUNCATE TABLE car_images;
TRUNCATE TABLE car_prices;
TRUNCATE TABLE cars;
TRUNCATE TABLE brands;
TRUNCATE TABLE categories;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Insert Users (Password is '123456' for all)
-- Verified BCrypt hash for '123456': $2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.
INSERT INTO users (id, full_name, email, password, role, phone, city, district, detailed_address, status, created_at) VALUES
(1, 'System Admin', 'admin@gmail.com', '$2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.', 'ADMIN', '0900000001', 'Hà Nội', 'Cầu Giấy', '1 Duy Tân', 'ACTIVE', NOW()),
(2, 'Chủ xe A', 'owner_a@gmail.com', '$2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.', 'OWNER', '0900000002', 'TP. Hồ Chí Minh', 'Quận 1', '100 Lê Lợi', 'ACTIVE', NOW()),
(3, 'Chủ xe B', 'owner_b@gmail.com', '$2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.', 'OWNER', '0900000003', 'Đà Nẵng', 'Hải Châu', '50 Bạch Đằng', 'ACTIVE', NOW()),
(4, 'Khách hàng C', 'customer_c@gmail.com', '$2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.', 'CUSTOMER', '0900000004', 'Hà Nội', 'Đống Đa', '20 Chùa Bộc', 'ACTIVE', NOW()),
(5, 'Khách hàng D', 'customer_d@gmail.com', '$2a$10$Y5HeTr5K2Dr8guxHSFiNWubjZutUT7SXTHwIfZf7G9OTHCp8.qSj.', 'CUSTOMER', '0900000005', 'TP. Hồ Chí Minh', 'Quận 7', '15 Nguyễn Lương Bằng', 'ACTIVE', NOW());

-- 2. Insert Brands
INSERT INTO brands (brand_id, name) VALUES
(1, 'Toyota'), (2, 'Honda'), (3, 'BMW'), (4, 'Mercedes-Benz'), (5, 'VinFast'), (6, 'Hyundai');

-- 3. Insert Categories
INSERT INTO categories (category_id, name, description) VALUES
(1, 'Sedan', 'Xe 4 chỗ gầm thấp'), (2, 'SUV', 'Xe gầm cao 5-7 chỗ'), (3, 'Hatchback', 'Xe nhỏ gọn'), (4, 'Luxury', 'Xe hạng sang');

-- 4. Insert Cars
INSERT INTO cars (car_id, owner_id, brand_id, category_id, name, color, year, license_plate, description, location_city, location_district, status, created_at) VALUES
(1, 2, 1, 2, 'Toyota Fortuner', 'Trắng', 2022, '30H-12345', '7 chỗ rộng rãi', 'Hà Nội', 'Cầu Giấy', 'AVAILABLE', NOW()),
(2, 2, 3, 1, 'BMW 320i', 'Đen', 2021, '30A-99999', 'Xe sang', 'Hà Nội', 'Hoàn Kiếm', 'AVAILABLE', NOW()),
(3, 2, 5, 2, 'VinFast VF8', 'Xanh dương', 2023, '51G-88888', 'Xe điện', 'TP. Hồ Chí Minh', 'Quận 1', 'AVAILABLE', NOW());

-- 5. Insert Car Prices
INSERT INTO car_prices (id, car_id, price_hour, price_day, price_month) VALUES
(1, 1, 150000, 1200000, 25000000), (2, 2, 300000, 2500000, 60000000), (3, 3, 200000, 1500000, 35000000);

-- 6. Insert Car Images
INSERT INTO car_images (id, car_id, image_url) VALUES
(1, 1, 'https://images.unsplash.com/photo-1594502184342-2e12f877aa73?auto=format&fit=crop&w=800'),
(2, 2, 'https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=800'),
(3, 3, 'https://images.unsplash.com/photo-1617788130012-05baac7c844b?auto=format&fit=crop&w=800');

-- 8. Insert some Bookings
INSERT INTO bookings (booking_id, customer_id, car_id, start_date, end_date, total_price, booking_type, status, created_at) VALUES
(1, 4, 1, '2024-03-10 08:00:00', '2024-03-12 20:00:00', 2400000, 'DAY', 'COMPLETED', '2024-03-09 10:00:00');

-- 9. Insert Payments
INSERT INTO payments (payment_id, booking_id, amount, payment_method, payment_status, transaction_id, created_at) VALUES
(1, 1, 2400000, 'CASH', 'COMPLETED', 'TRANS-001', '2024-03-12 20:30:00');

-- 10. Insert Feedbacks
INSERT INTO feedbacks (feedback_id, booking_id, car_id, customer_id, rating, comment, created_at) VALUES
(1, 1, 1, 4, 5, 'Xe đi rất êm!', '2024-03-12 21:00:00');
