-- FarmLink schema (MySQL 8). Replaces the Supabase tables: user_roles + profiles are merged into users.

CREATE TABLE users (
    id             CHAR(36)     NOT NULL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    phone          VARCHAR(30),
    location       VARCHAR(255),
    avatar_url     VARCHAR(500),
    license_number VARCHAR(100),
    is_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    latitude       DOUBLE,
    longitude      DOUBLE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE products (
    id                 CHAR(36)       NOT NULL PRIMARY KEY,
    farmer_id          CHAR(36)       NOT NULL,
    name               VARCHAR(200)   NOT NULL,
    description        TEXT,
    category           VARCHAR(20)    NOT NULL DEFAULT 'OTHER',
    price_per_unit     DECIMAL(10, 2) NOT NULL,
    unit               VARCHAR(20)    NOT NULL DEFAULT 'kg',
    quantity_available DECIMAL(10, 2) NOT NULL DEFAULT 0,
    image_url          VARCHAR(500),
    is_available       BOOLEAN        NOT NULL DEFAULT TRUE,
    location           VARCHAR(255),
    shelf_life_hours   INT            NOT NULL DEFAULT 168,
    latitude           DOUBLE,
    longitude          DOUBLE,
    created_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_products_farmer FOREIGN KEY (farmer_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_products_farmer ON products (farmer_id);
CREATE INDEX idx_products_category ON products (category);

CREATE TABLE orders (
    id                  CHAR(36)       NOT NULL PRIMARY KEY,
    retailer_id         CHAR(36)       NOT NULL,
    farmer_id           CHAR(36)       NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    notes               TEXT,
    delivery_address    VARCHAR(500),
    delivery_latitude   DOUBLE,
    delivery_longitude  DOUBLE,
    payment_status      VARCHAR(20)    NOT NULL DEFAULT 'UNPAID',
    razorpay_order_id   VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_orders_retailer FOREIGN KEY (retailer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_farmer FOREIGN KEY (farmer_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_orders_retailer ON orders (retailer_id);
CREATE INDEX idx_orders_farmer ON orders (farmer_id);
CREATE INDEX idx_orders_razorpay ON orders (razorpay_order_id);

CREATE TABLE order_items (
    id             CHAR(36)       NOT NULL PRIMARY KEY,
    order_id       CHAR(36)       NOT NULL,
    product_id     CHAR(36)       NOT NULL,
    product_name   VARCHAR(200)   NOT NULL,
    quantity       DECIMAL(10, 2) NOT NULL,
    price_per_unit DECIMAL(10, 2) NOT NULL,
    total_price    DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);
CREATE INDEX idx_items_order ON order_items (order_id);
CREATE INDEX idx_items_product ON order_items (product_id);

CREATE TABLE reviews (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    order_id    CHAR(36)     NOT NULL,
    reviewer_id CHAR(36)     NOT NULL,
    farmer_id   CHAR(36)     NOT NULL,
    rating      INT          NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT uq_reviews_order_reviewer UNIQUE (order_id, reviewer_id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_farmer FOREIGN KEY (farmer_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_reviews_farmer ON reviews (farmer_id);

CREATE TABLE notifications (
    id           CHAR(36)      NOT NULL PRIMARY KEY,
    user_id      CHAR(36)      NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    type         VARCHAR(30)   NOT NULL DEFAULT 'GENERAL',
    reference_id CHAR(36),
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at);
