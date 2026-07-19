-- Test schema for JdbcDatabaseAdapterH2Test
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);
INSERT INTO users (id, name) VALUES (1, 'Ana'), (2, 'Carlos'), (3, 'Sofia');
