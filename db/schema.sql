-- =================================================================
-- AI SQL Query Generator — schema de ejemplo (e-commerce)
-- =================================================================
-- 4 tablas relacionadas: users, products, orders, order_items
-- Idempotente: se puede ejecutar varias veces (DROP IF EXISTS).
-- =================================================================

-- Limpieza: borramos en orden inverso por las FKs
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =================================================================
-- USERS
-- =================================================================
CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    country     VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_country ON users(country);

-- =================================================================
-- PRODUCTS
-- =================================================================
CREATE TABLE products (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(150)   NOT NULL,
    category    VARCHAR(50)    NOT NULL,
    price       NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    stock       INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category);

-- =================================================================
-- ORDERS
-- =================================================================
CREATE TABLE orders (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending', 'paid', 'shipped', 'delivered', 'cancelled')),
    total       NUMERIC(10, 2) NOT NULL CHECK (total >= 0),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- =================================================================
-- ORDER_ITEMS
-- =================================================================
CREATE TABLE order_items (
    id          SERIAL PRIMARY KEY,
    order_id    INTEGER        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  INTEGER        NOT NULL REFERENCES products(id),
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10, 2) NOT NULL CHECK (unit_price > 0)
);

CREATE INDEX idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- =================================================================
-- SEED DATA
-- =================================================================
-- 20 usuarios, 50 productos, 100 orders, ~300 order_items.
-- Datos realistas y variados para que las preguntas en lenguaje
-- natural tengan sentido (precios, países, fechas, categorías).
-- =================================================================

-- Users (20)
INSERT INTO users (name, email, country) VALUES
    ('Ana García',      'ana.garcia@example.com',       'Spain'),
    ('Carlos López',    'carlos.lopez@example.com',     'Mexico'),
    ('María Rodríguez', 'maria.rodriguez@example.com',  'Argentina'),
    ('Juan Martínez',   'juan.martinez@example.com',    'Spain'),
    ('Laura Sánchez',   'laura.sanchez@example.com',    'Colombia'),
    ('Pedro Gómez',     'pedro.gomez@example.com',      'Chile'),
    ('Sofía Fernández', 'sofia.fernandez@example.com',  'Spain'),
    ('Diego Torres',    'diego.torres@example.com',     'Peru'),
    ('Carmen Ruiz',     'carmen.ruiz@example.com',      'Spain'),
    ('Andrés Vargas',   'andres.vargas@example.com',    'Venezuela'),
    ('Lucía Morales',   'lucia.morales@example.com',    'Mexico'),
    ('Miguel Castro',   'miguel.castro@example.com',    'Spain'),
    ('Isabel Romero',   'isabel.romero@example.com',    'Argentina'),
    ('Fernando Reyes',  'fernando.reyes@example.com',   'Colombia'),
    ('Patricia Vega',   'patricia.vega@example.com',    'Spain'),
    ('Roberto Núñez',   'roberto.nunez@example.com',    'Chile'),
    ('Elena Méndez',    'elena.mendez@example.com',     'Spain'),
    ('Javier Soto',     'javier.soto@example.com',      'Mexico'),
    ('Rosa Guerrero',   'rosa.guerrero@example.com',    'Peru'),
    ('Manuel Delgado',  'manuel.delgado@example.com',   'Spain');

-- Products (50) — categorías variadas y precios realistas
INSERT INTO products (name, category, price, stock) VALUES
    ('Laptop Pro 14"',        'electronics', 1299.99, 25),
    ('Wireless Mouse',        'electronics',   29.99, 150),
    ('Mechanical Keyboard',   'electronics',   89.99,  80),
    ('USB-C Hub 7-port',      'electronics',   45.50,  60),
    ('27" 4K Monitor',        'electronics',  349.00,  30),
    ('Noise-cancelling HPs',  'electronics',  199.00,  45),
    ('Webcam 1080p',          'electronics',   59.99,  90),
    ('Desk Lamp LED',         'home',          39.99,  70),
    ('Office Chair',          'home',         249.00,  20),
    ('Standing Desk',         'home',         499.00,  15),
    ('Bookshelf 5-tier',      'home',         129.00,  25),
    ('Coffee Mug Ceramic',    'home',          12.50, 200),
    ('Throw Pillow',          'home',          24.99, 100),
    ('Plant Pot Terracotta',  'home',          18.00,  85),
    ('Yoga Mat',              'sports',        34.99,  60),
    ('Dumbbells 10kg pair',   'sports',        79.99,  40),
    ('Running Shoes',         'sports',       119.00,  55),
    ('Resistance Bands Set',  'sports',        19.99, 120),
    ('Water Bottle 1L',       'sports',        14.50, 150),
    ('Football',              'sports',        29.99,  70),
    ('The Pragmatic Programmer', 'books',      35.00,  50),
    ('Clean Code',            'books',         32.00,  45),
    ('Designing Data-Intensive Apps', 'books', 42.00,  40),
    ('SQL Antipatterns',      'books',         28.50,  35),
    ('Python Cookbook',       'books',         38.00,  50),
    ('Domain-Driven Design',  'books',         45.00,  30),
    ('Refactoring',           'books',         36.00,  40),
    ('The Mythical Man-Month','books',         22.00,  55),
    ('Headphones Stand',      'electronics',   15.99,  85),
    ('Phone Charger 20W',     'electronics',   19.99, 200),
    ('Tablet 10"',            'electronics',  299.00,  35),
    ('Smartwatch',            'electronics',  199.00,  60),
    ('Bluetooth Speaker',     'electronics',   79.99,  90),
    ('External SSD 1TB',      'electronics',  119.00,  70),
    ('Router WiFi 6',         'electronics',  149.00,  25),
    ('Bed Sheet Set Queen',   'home',          49.99,  80),
    ('Kitchen Knife Set',     'home',          89.00,  40),
    ('Blender 500W',          'home',          69.99,  55),
    ('Air Purifier',          'home',         179.00,  30),
    ('Wall Clock',            'home',          29.99,  60),
    ('Basketball',            'sports',        24.99,  80),
    ('Tennis Racket',         'sports',        89.00,  35),
    ('Cycling Helmet',        'sports',        64.99,  45),
    ('Backpack 30L',          'sports',        54.99, 100),
    ('Sunglasses Polarized',  'sports',        44.99,  90),
    ('Notebook A5',           'books',          8.50, 250),
    ('Fountain Pen',          'books',         28.00,  60),
    ('Desk Pad XL',           'home',          34.99,  50),
    ('Cable Organizer Kit',   'home',          12.99, 150),
    ('Wireless Charger',      'electronics',   24.99, 110);

-- Orders (100) — distribuidas en los últimos 6 meses, varios status
-- Generadas de forma que las fechas son razonables y los user_id
-- y status son variados.
INSERT INTO orders (user_id, status, total, created_at) VALUES
    (1,  'delivered', 1329.98, NOW() - INTERVAL '180 days'),
    (1,  'delivered',   89.99, NOW() - INTERVAL '150 days'),
    (1,  'paid',       199.00, NOW() - INTERVAL '30 days'),
    (2,  'delivered',  349.00, NOW() - INTERVAL '170 days'),
    (2,  'cancelled',   45.50, NOW() - INTERVAL '120 days'),
    (3,  'delivered',  119.00, NOW() - INTERVAL '160 days'),
    (3,  'shipped',     29.99, NOW() - INTERVAL '10 days'),
    (3,  'paid',        59.99, NOW() - INTERVAL '5 days'),
    (4,  'delivered',  249.00, NOW() - INTERVAL '140 days'),
    (4,  'delivered',   34.99, NOW() - INTERVAL '90 days'),
    (5,  'pending',    179.00, NOW() - INTERVAL '2 days'),
    (6,  'delivered',   79.99, NOW() - INTERVAL '100 days'),
    (6,  'paid',        42.00, NOW() - INTERVAL '15 days'),
    (7,  'delivered',  299.00, NOW() - INTERVAL '130 days'),
    (7,  'delivered',   24.99, NOW() - INTERVAL '60 days'),
    (7,  'shipped',     89.99, NOW() - INTERVAL '7 days'),
    (8,  'delivered',   89.00, NOW() - INTERVAL '110 days'),
    (9,  'cancelled',  1299.99, NOW() - INTERVAL '80 days'),
    (9,  'paid',        39.99, NOW() - INTERVAL '4 days'),
    (10, 'delivered',  199.00, NOW() - INTERVAL '95 days'),
    (10, 'delivered',   64.99, NOW() - INTERVAL '40 days'),
    (11, 'pending',     89.00, NOW() - INTERVAL '1 day'),
    (12, 'delivered',  149.00, NOW() - INTERVAL '85 days'),
    (13, 'delivered',   19.99, NOW() - INTERVAL '50 days'),
    (13, 'paid',       119.00, NOW() - INTERVAL '20 days'),
    (14, 'shipped',     44.99, NOW() - INTERVAL '6 days'),
    (15, 'delivered',   79.99, NOW() - INTERVAL '75 days'),
    (15, 'delivered',   34.99, NOW() - INTERVAL '35 days'),
    (16, 'paid',       179.00, NOW() - INTERVAL '12 days'),
    (17, 'delivered',   45.00, NOW() - INTERVAL '115 days'),
    (17, 'delivered',   29.99, NOW() - INTERVAL '55 days'),
    (18, 'delivered',   89.99, NOW() - INTERVAL '125 days'),
    (19, 'paid',        24.99, NOW() - INTERVAL '8 days'),
    (20, 'delivered',  299.00, NOW() - INTERVAL '105 days'),
    (20, 'cancelled',   89.99, NOW() - INTERVAL '70 days'),
    -- Más orders para llegar a 100. Simplificamos la lista repitiendo
    -- el patrón con un script en Python más abajo; aquí solo dejamos
    -- los primeros 36 con datos cuidadosos.
    (1,  'delivered',   14.50, NOW() - INTERVAL '165 days'),
    (2,  'delivered',   32.00, NOW() - INTERVAL '155 days'),
    (3,  'delivered',   18.00, NOW() - INTERVAL '145 days'),
    (4,  'delivered',   28.50, NOW() - INTERVAL '135 days'),
    (5,  'delivered',   38.00, NOW() - INTERVAL '125 days'),
    (6,  'delivered',   45.00, NOW() - INTERVAL '115 days'),
    (7,  'delivered',   36.00, NOW() - INTERVAL '105 days'),
    (8,  'delivered',   22.00, NOW() - INTERVAL '95 days'),
    (9,  'delivered',   15.99, NOW() - INTERVAL '85 days'),
    (10, 'delivered',   19.99, NOW() - INTERVAL '75 days'),
    (11, 'delivered',  299.00, NOW() - INTERVAL '65 days'),
    (12, 'delivered',  199.00, NOW() - INTERVAL '55 days'),
    (13, 'delivered',   79.99, NOW() - INTERVAL '45 days'),
    (14, 'delivered',  119.00, NOW() - INTERVAL '35 days'),
    (15, 'delivered',  149.00, NOW() - INTERVAL '25 days'),
    (16, 'delivered',   49.99, NOW() - INTERVAL '22 days'),
    (17, 'delivered',   89.00, NOW() - INTERVAL '19 days'),
    (18, 'delivered',   69.99, NOW() - INTERVAL '16 days'),
    (19, 'delivered',  179.00, NOW() - INTERVAL '13 days'),
    (20, 'delivered',   29.99, NOW() - INTERVAL '11 days'),
    (1,  'shipped',     54.99, NOW() - INTERVAL '9 days'),
    (2,  'shipped',     44.99, NOW() - INTERVAL '8 days'),
    (3,  'paid',        64.99, NOW() - INTERVAL '7 days'),
    (4,  'paid',        89.00, NOW() - INTERVAL '6 days'),
    (5,  'paid',        34.50, NOW() - INTERVAL '5 days'),
    (6,  'pending',    129.50, NOW() - INTERVAL '4 days'),
    (7,  'pending',     45.00, NOW() - INTERVAL '3 days'),
    (8,  'pending',     89.00, NOW() - INTERVAL '2 days'),
    (9,  'delivered',   35.00, NOW() - INTERVAL '90 days'),
    (10, 'delivered',   42.00, NOW() - INTERVAL '88 days'),
    (11, 'delivered',   32.00, NOW() - INTERVAL '86 days'),
    (12, 'delivered',   28.50, NOW() - INTERVAL '84 days'),
    (13, 'delivered',   38.00, NOW() - INTERVAL '82 days'),
    (14, 'delivered',   45.00, NOW() - INTERVAL '80 days'),
    (15, 'delivered',   36.00, NOW() - INTERVAL '78 days'),
    (16, 'delivered',   22.00, NOW() - INTERVAL '76 days'),
    (17, 'delivered',   15.99, NOW() - INTERVAL '74 days'),
    (18, 'delivered',   19.99, NOW() - INTERVAL '72 days'),
    (19, 'delivered',  299.00, NOW() - INTERVAL '70 days'),
    (20, 'delivered',  199.00, NOW() - INTERVAL '68 days'),
    (1,  'delivered',   79.99, NOW() - INTERVAL '66 days'),
    (2,  'delivered',  119.00, NOW() - INTERVAL '64 days'),
    (3,  'delivered',  149.00, NOW() - INTERVAL '62 days'),
    (4,  'delivered',   49.99, NOW() - INTERVAL '60 days'),
    (5,  'delivered',   89.00, NOW() - INTERVAL '58 days'),
    (6,  'delivered',   69.99, NOW() - INTERVAL '56 days'),
    (7,  'delivered',  179.00, NOW() - INTERVAL '54 days'),
    (8,  'delivered',   29.99, NOW() - INTERVAL '52 days'),
    (9,  'delivered',   54.99, NOW() - INTERVAL '50 days'),
    (10, 'delivered',   44.99, NOW() - INTERVAL '48 days'),
    (11, 'delivered',   64.99, NOW() - INTERVAL '46 days'),
    (12, 'delivered',   89.00, NOW() - INTERVAL '44 days'),
    (13, 'delivered',   34.50, NOW() - INTERVAL '42 days'),
    (14, 'delivered',  129.50, NOW() - INTERVAL '40 days'),
    (15, 'delivered',   45.00, NOW() - INTERVAL '38 days'),
    (16, 'delivered',   89.00, NOW() - INTERVAL '36 days'),
    (17, 'delivered',   35.00, NOW() - INTERVAL '34 days'),
    (18, 'delivered',   42.00, NOW() - INTERVAL '32 days'),
    (19, 'delivered',   32.00, NOW() - INTERVAL '30 days'),
    (20, 'delivered',   28.50, NOW() - INTERVAL '28 days'),
    (1,  'delivered',   38.00, NOW() - INTERVAL '26 days'),
    (2,  'paid',        45.00, NOW() - INTERVAL '24 days'),
    (3,  'paid',        36.00, NOW() - INTERVAL '22 days'),
    (4,  'paid',        22.00, NOW() - INTERVAL '20 days'),
    (5,  'paid',        15.99, NOW() - INTERVAL '18 days'),
    (6,  'paid',        19.99, NOW() - INTERVAL '17 days'),
    (7,  'paid',       299.00, NOW() - INTERVAL '16 days'),
    (8,  'pending',    199.00, NOW() - INTERVAL '14 days'),
    (9,  'pending',     79.99, NOW() - INTERVAL '12 days'),
    (10, 'pending',    119.00, NOW() - INTERVAL '11 days');

-- Order items (3 por orden, aprox 300 en total).
-- Generamos 3 items por cada uno de los 100 orders.
-- Para no escribir 300 inserts a mano, usamos generate_series y un
-- poco de aleatoriedad determinista.
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT
    o.id,
    ((o.id * 7 + gs) % 50) + 1                                   AS product_id,  -- rota entre los 50 productos
    ((o.id + gs) % 3) + 1                                       AS quantity,    -- 1 a 3
    p.price
FROM orders o
CROSS JOIN generate_series(0, 2) AS gs
JOIN products p ON p.id = ((o.id * 7 + gs) % 50) + 1;
