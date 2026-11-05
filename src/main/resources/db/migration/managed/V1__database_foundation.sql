-- Flyway owns DDL. Defaults inherit the MySQL server's InnoDB/utf8mb4 settings.
-- Timestamps represent UTC. No existing schema is dropped or baselined.
CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('ADMIN', 'CUSTOMER'))
);

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
);
CREATE INDEX idx_users_role ON users(role_id);

CREATE TABLE brands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
);

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
);
CREATE INDEX idx_vehicles_brand ON vehicles(brand_id);
CREATE INDEX idx_vehicles_status_price ON vehicles(availability_status, price);

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
);

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
);
CREATE INDEX idx_appointments_user_time ON appointments(user_id, appointment_at);
CREATE INDEX idx_appointments_vehicle_time ON appointments(vehicle_id, appointment_at);
CREATE INDEX idx_appointments_status_time ON appointments(status, appointment_at);

CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, vehicle_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);
CREATE INDEX idx_favorites_vehicle ON favorites(vehicle_id);
