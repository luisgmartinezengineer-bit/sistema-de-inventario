-- ============================================================
--  SCRIPT DE BASE DE DATOS - SISTEMA DE INVENTARIO v2
--  Motor: MySQL / MariaDB (XAMPP)
--  Instrucciones:
--    1. Abre phpMyAdmin en http://localhost/phpmyadmin
--    2. Haz clic en "SQL" (pestaña superior)
--    3. Pega todo este contenido y haz clic en "Continuar"
-- ============================================================

CREATE DATABASE IF NOT EXISTS inventario
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE inventario;

-- ------------------------------------------------------------
-- CATEGORIAS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PRODUCTOS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(500),
    category_id BIGINT,
    price       DECIMAL(12,2)  NOT NULL,
    tax_rate    DECIMAL(5,2)   NOT NULL DEFAULT 19.00,
    barcode     VARCHAR(100)   UNIQUE,
    stock       INT            NOT NULL DEFAULT 0,
    min_stock   INT            NOT NULL DEFAULT 5,
    unit        VARCHAR(50),
    active      TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- VENTAS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number   VARCHAR(30) UNIQUE,
    date             DATETIME       NOT NULL,
    subtotal         DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    tax_amount       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    total            DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    payment_method   ENUM('EFECTIVO','TARJETA','TRANSFERENCIA','CREDITO') NOT NULL DEFAULT 'EFECTIVO',
    notes            VARCHAR(500)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ITEMS DE VENTA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sale_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id    BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(12,2)  NOT NULL,
    subtotal   DECIMAL(12,2)  NOT NULL,
    tax_rate   DECIMAL(5,2)   NOT NULL DEFAULT 19.00,
    tax_amount DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_saleitem_sale    FOREIGN KEY (sale_id)    REFERENCES sales(id)    ON DELETE CASCADE,
    CONSTRAINT fk_saleitem_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- MOVIMIENTOS DE STOCK (historial)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_movements (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    type         ENUM('ENTRY','EXIT','ADJUSTMENT') NOT NULL,
    quantity     INT          NOT NULL,
    stock_before INT          NOT NULL,
    stock_after  INT          NOT NULL,
    date         DATETIME     NOT NULL,
    reason       VARCHAR(255),
    sale_id      BIGINT,
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ALERTAS DE STOCK BAJO
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_alerts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT    NOT NULL,
    current_stock INT       NOT NULL,
    min_stock     INT       NOT NULL,
    created_at    DATETIME  NOT NULL,
    resolved      TINYINT(1) NOT NULL DEFAULT 0,
    resolved_at   DATETIME,
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- USUARIOS DEL SISTEMA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_users (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(100) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    role      ENUM('ADMIN','SUPERVISOR','VENDEDOR') NOT NULL DEFAULT 'VENDEDOR',
    active    TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- CLIENTES
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(200) NOT NULL,
    document VARCHAR(50),
    email    VARCHAR(150),
    phone    VARCHAR(30),
    address  VARCHAR(300),
    active   TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- CAJAS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cash_registers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    seller_id       BIGINT,
    status          ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'CLOSED',
    opened_at       DATETIME,
    closed_at       DATETIME,
    initial_amount  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_sales     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_expenses  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes           VARCHAR(500),
    CONSTRAINT fk_caja_seller FOREIGN KEY (seller_id) REFERENCES app_users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- CONFIGURACIÓN DE EMPRESA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_config (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    razon_social           VARCHAR(200) NOT NULL DEFAULT 'Mi Empresa',
    nit                    VARCHAR(20)  NOT NULL DEFAULT '000000000',
    digito_verificacion    VARCHAR(2)   NOT NULL DEFAULT '0',
    address                VARCHAR(300),
    city                   VARCHAR(100),
    department             VARCHAR(100),
    phone                  VARCHAR(50),
    email                  VARCHAR(150),
    website                VARCHAR(200),
    regime                 ENUM('RESPONSABLE_IVA','NO_RESPONSABLE_IVA','GRAN_CONTRIBUYENTE','REGIMEN_SIMPLE') NOT NULL DEFAULT 'RESPONSABLE_IVA',
    dian_resolution_number VARCHAR(50),
    dian_resolution_date   DATE,
    dian_range_from        BIGINT,
    dian_range_to          BIGINT,
    invoice_prefix         VARCHAR(10)  NOT NULL DEFAULT 'FV',
    current_invoice_number BIGINT       NOT NULL DEFAULT 0,
    ticket_footer          VARCHAR(500)
) ENGINE=InnoDB;

-- Insertar configuración por defecto si no existe
INSERT INTO company_config (razon_social, nit, digito_verificacion, invoice_prefix, current_invoice_number)
SELECT 'Mi Empresa S.A.S', '900123456', '7', 'FV', 0
WHERE NOT EXISTS (SELECT 1 FROM company_config);

-- Añadir columnas nuevas a sales (solo si no existen)
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS customer_id      BIGINT,
    ADD COLUMN IF NOT EXISTS cash_register_id BIGINT,
    ADD COLUMN IF NOT EXISTS seller_id        BIGINT;

ALTER TABLE sales
    ADD CONSTRAINT IF NOT EXISTS fk_sale_customer      FOREIGN KEY (customer_id)      REFERENCES customers(id)      ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_sale_cash_register FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id) ON DELETE SET NULL,
    ADD CONSTRAINT IF NOT EXISTS fk_sale_seller        FOREIGN KEY (seller_id)        REFERENCES app_users(id)      ON DELETE SET NULL;

-- ============================================================
-- DATOS DE EJEMPLO (opcional — borra si no los quieres)
-- ============================================================
INSERT INTO categories (name, description) VALUES
    ('Electrónica',   'Equipos y accesorios electrónicos'),
    ('Alimentos',     'Productos alimenticios y bebidas'),
    ('Ropa',          'Prendas de vestir'),
    ('Herramientas',  'Herramientas y ferretería');

INSERT INTO products (name, description, category_id, price, stock, min_stock, unit) VALUES
    ('Laptop HP 15"',      'Laptop Intel Core i5, 8GB RAM',  1, 7500.00,  10, 3,  'unidad'),
    ('Mouse Inalámbrico',  'Mouse USB receptor nano',         1,  150.00,  50, 10, 'unidad'),
    ('Arroz 1kg',          'Arroz blanco grano largo',        2,   25.00, 200, 30, 'kg'),
    ('Aceite 1L',          'Aceite vegetal',                  2,   45.00,   4, 20, 'litro'),
    ('Camiseta Básica',    'Algodón 100%, talla M',           3,  120.00,  30, 5,  'unidad'),
    ('Destornillador Set', 'Juego 12 piezas',                 4,  180.00,   2, 5,  'set');

-- Alerta de ejemplo para productos con stock bajo
INSERT INTO stock_alerts (product_id, current_stock, min_stock, created_at, resolved) VALUES
    (4, 4, 20, NOW(), 0),
    (6, 2, 5,  NOW(), 0);
