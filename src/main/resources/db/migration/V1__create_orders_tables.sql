-- V1: Create orders and order_items tables

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID        NOT NULL,
    status      VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency    CHAR(3)     NOT NULL,
    shipping_address TEXT,
    payment_method   VARCHAR(50),
    notes       TEXT,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status      ON orders (status);
CREATE INDEX idx_orders_created_at  ON orders (created_at DESC);

CREATE TABLE order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id      UUID           NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    sku             VARCHAR(100)   NOT NULL,
    quantity        INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(10, 2) NOT NULL CHECK (unit_price > 0),
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
