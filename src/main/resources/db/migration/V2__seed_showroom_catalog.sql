-- Snapshot of InMemoryVehicleRepository; EUR comes from the existing templates.
-- Flyway records this version exactly once; there is no startup overwrite/upsert.
-- UNSPECIFIED avoids inventing inventory availability. No accounts are seeded.
INSERT INTO roles (name) VALUES ('ADMIN'), ('CUSTOMER');

INSERT INTO brands (name, slug) VALUES
    ('Ferrari', 'ferrari'), ('Lamborghini', 'lamborghini'), ('McLaren', 'mclaren'),
    ('Porsche', 'porsche'), ('Aston Martin', 'aston-martin'), ('Rimac', 'rimac'),
    ('Koenigsegg', 'koenigsegg'), ('Mercedes-AMG', 'mercedes-amg');

INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Ferrari SF90 Stradale', 'ferrari-sf90-stradale',
       'A landmark plug-in hybrid supercar balancing extreme performance with remarkable composure.',
       2025, 625000, 'EUR', 986, 340, 2.5, 'UNSPECIFIED' FROM brands WHERE slug = 'ferrari';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Lamborghini Revuelto', 'lamborghini-revuelto',
       'V12 theatre meets a sophisticated hybrid system in a shape engineered for maximum presence.',
       2025, 608000, 'EUR', 1001, 350, 2.5, 'UNSPECIFIED' FROM brands WHERE slug = 'lamborghini';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'McLaren 750S', 'mclaren-750s',
       'Focused engineering and low mass create a deeply connected, road-ready supercar experience.',
       2025, 324000, 'EUR', 740, 332, 2.8, 'UNSPECIFIED' FROM brands WHERE slug = 'mclaren';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Porsche 911 GT3 RS', 'porsche-911-gt3-rs',
       'A precision instrument shaped by downforce, response, and the unmistakable character of a flat-six.',
       2025, 285000, 'EUR', 518, 296, 3.2, 'UNSPECIFIED' FROM brands WHERE slug = 'porsche';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Aston Martin Valhalla', 'aston-martin-valhalla',
       'A mid-engine hybrid flagship combining sculptural design, active aero, and devastating pace.',
       2026, 850000, 'EUR', 1064, 350, 2.5, 'UNSPECIFIED' FROM brands WHERE slug = 'aston-martin';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Rimac Nevera', 'rimac-nevera',
       'Four independent motors deliver extraordinary control and a new definition of instant performance.',
       2025, 2400000, 'EUR', 1914, 412, 1.9, 'UNSPECIFIED' FROM brands WHERE slug = 'rimac';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Koenigsegg Jesko Attack', 'koenigsegg-jesko-attack',
       'An uncompromising high-downforce machine built around airflow, low mass, and immense power.',
       2025, 3000000, 'EUR', 1578, 480, 2.5, 'UNSPECIFIED' FROM brands WHERE slug = 'koenigsegg';
INSERT INTO vehicles (brand_id, name, slug, description, model_year, price, currency,
                      horsepower, top_speed_kmh, acceleration_0_100, availability_status)
SELECT id, 'Mercedes-AMG ONE', 'mercedes-amg-one',
       'A road-going expression of Formula-derived hybrid engineering with extraordinary technical depth.',
       2025, 2720000, 'EUR', 1049, 352, 2.9, 'UNSPECIFIED' FROM brands WHERE slug = 'mercedes-amg';

-- Only hero metadata is mirrored. The 24-image Ferrari manifest stays authoritative.
INSERT INTO vehicle_images (vehicle_id, image_url, alt_text, category, display_order, is_primary)
SELECT id, CONCAT('/assets/cars/', slug, '/hero.webp'), name, 'HERO', 0, 1 FROM vehicles;
