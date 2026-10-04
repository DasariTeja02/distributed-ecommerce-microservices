-- Initialize PostgreSQL databases for distributed microservices
-- Ensures strict database-per-service isolation

SELECT 'CREATE DATABASE ecommerce_order_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ecommerce_order_db')\gexec

SELECT 'CREATE DATABASE ecommerce_inventory_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ecommerce_inventory_db')\gexec

SELECT 'CREATE DATABASE ecommerce_payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ecommerce_payment_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE ecommerce_order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_payment_db TO postgres;
