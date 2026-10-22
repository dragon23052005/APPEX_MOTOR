-- APEX MOTORS: chay truc tiep trong MySQL Workbench / IntelliJ Database.
-- MySQL 8.0.16+ (da chay thu tren MySQL 8.0). Ket noi den MySQL server truoc,
-- sau do chay TOAN BO file tren cung mot connection, dung khi gap loi.
-- Tai khoan can quyen CREATE DATABASE va tao bang/index/khoa ngoai.
-- File nay tao database apex_motors va 7 bang, chua nap du lieu mau.
-- Chi chay mot lan tren database moi hoac rong. Neu bang da ton tai,
-- script bao loi; khong xoa, ghi de hay tu dong sua du lieu hien co.
-- Sau khi thanh cong, co the chay V2__seed_showroom_catalog.sql
-- tren cung connection voi database apex_motors dang duoc chon.
--
-- Spring Boot/Flyway dung cac ban goc trong db/migration/managed.
-- Chon MOT cach khoi tao: chay thu cong HOAC de Flyway tu chay.
-- Chay thu cong khong tao flyway_schema_history; profile mysql se tu choi
-- database nay cho den khi co ke hoach tiep nhan schema da duoc kiem tra.
-- All DATETIME values represent UTC; set this on later client sessions too.

CREATE DATABASE IF NOT EXISTS apex_motors
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE apex_motors;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+00:00';

-- 1. Roles must exist before users.
CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('ADMIN', 'CUSTOMER'))
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Users reference roles; passwords must already be encoded hashes.
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(32),
    enabled BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_users_role ON users(role_id);

-- 3. Brands must exist before vehicles.
CREATE TABLE brands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Vehicle prices use exact decimal values.
CREATE TABLE vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    brand_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description VARCHAR(4000),
    model_year INT,
    price DECIMAL(15,2),
    currency VARCHAR(3),
    horsepower INT,
    top_speed_kmh INT,
    acceleration_0_100 DECIMAL(5,2),
    availability_status VARCHAR(24) NOT NULL DEFAULT 'UNSPECIFIED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    CONSTRAINT uk_vehicles_slug UNIQUE (slug),
    CONSTRAINT fk_vehicles_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE RESTRICT,
    CONSTRAINT ck_vehicles_year CHECK (model_year BETWEEN 1886 AND 9999),
    CONSTRAINT ck_vehicles_price CHECK (price >= 0),
    CONSTRAINT ck_vehicles_currency CHECK ((price IS NULL AND currency IS NULL) OR (price IS NOT NULL AND currency IS NOT NULL)),
    CONSTRAINT ck_vehicles_power CHECK (horsepower >= 0),
    CONSTRAINT ck_vehicles_speed CHECK (top_speed_kmh >= 0),
    CONSTRAINT ck_vehicles_acceleration CHECK (acceleration_0_100 > 0),
    CONSTRAINT ck_vehicles_status CHECK (availability_status IN ('UNSPECIFIED', 'AVAILABLE', 'RESERVED', 'SOLD'))
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_vehicles_brand ON vehicles(brand_id);
CREATE INDEX idx_vehicles_status_price ON vehicles(availability_status, price);

-- 5. Store image URLs and metadata, not image files.
CREATE TABLE vehicle_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    alt_text VARCHAR(255) NOT NULL,
    category VARCHAR(24) NOT NULL,
    display_order INT NOT NULL,
    is_primary BIT NOT NULL DEFAULT 0,
    CONSTRAINT pk_vehicle_images PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_images_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT uk_vehicle_images_url UNIQUE (vehicle_id, image_url),
    CONSTRAINT uk_vehicle_images_order UNIQUE (vehicle_id, display_order),
    CONSTRAINT ck_vehicle_images_order CHECK (display_order >= 0),
    CONSTRAINT ck_vehicle_images_category CHECK (category IN ('HERO', 'EXTERIOR', 'DETAILS', 'INTERIOR'))
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Appointment history prevents deletion of its user or vehicle.
CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    appointment_at DATETIME(6) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    customer_note VARCHAR(2000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT ck_appointments_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_appointments_user_time ON appointments(user_id, appointment_at);
CREATE INDEX idx_appointments_vehicle_time ON appointments(vehicle_id, appointment_at);
CREATE INDEX idx_appointments_status_time ON appointments(status, appointment_at);

-- 7. A user can favorite each vehicle only once.
CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, vehicle_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_favorites_vehicle ON favorites(vehicle_id);

-- Refresh Schemas / Synchronize in your SQL client to see these seven tables.
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'apex_motors'
ORDER BY TABLE_NAME;
