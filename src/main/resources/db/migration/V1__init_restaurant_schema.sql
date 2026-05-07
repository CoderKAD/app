CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- USERS
-- =========================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    roles VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (user_name),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_roles
        CHECK (roles IN ('ADMIN', 'CUSTOMER', 'KITCHEN', 'CASHIER', 'DELIVERY'))
);

CREATE INDEX idx_users_username ON users (user_name);
CREATE INDEX idx_users_email ON users (email);

-- =========================
-- STAFFS
-- =========================
CREATE TABLE staffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2),
    position VARCHAR(100) NOT NULL,
    date_joined DATE,
    date_left DATE,
    cin VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID UNIQUE,
    CONSTRAINT uq_staffs_cin UNIQUE (cin),
    CONSTRAINT fk_staffs_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_staff_user ON staffs (user_id);
CREATE INDEX idx_staff_position ON staffs (position);

-- =========================
-- CATEGORIES
-- =========================
CREATE TABLE categories_menu (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_menu_name UNIQUE (category_name),
    CONSTRAINT uq_categories_menu_sort_order UNIQUE (sort_order),
    CONSTRAINT chk_categories_menu_sort_order CHECK (sort_order > 0)
);

CREATE INDEX idx_categories_menu_name ON categories_menu (category_name);
CREATE INDEX idx_categories_menu_active ON categories_menu (active);

-- =========================
-- MENU ITEMS
-- =========================
CREATE TABLE menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url TEXT,
    prep_station VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    category_id UUID,
    CONSTRAINT fk_menu_items_category
        FOREIGN KEY (category_id) REFERENCES categories_menu (id) ON DELETE SET NULL,
    CONSTRAINT chk_menu_items_price CHECK (price > 0)
);

CREATE INDEX idx_menu_items_category ON menu_items (category_id);
CREATE INDEX idx_menu_items_active ON menu_items (active);
CREATE INDEX idx_menu_items_name ON menu_items (name);

-- =========================
-- RESTAURANT TABLES
-- =========================
CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label VARCHAR(50) NOT NULL,
    seats INTEGER NOT NULL,
    public_code VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id UUID,
    CONSTRAINT uq_restaurant_tables_public_code UNIQUE (public_code),
    CONSTRAINT fk_restaurant_tables_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_restaurant_tables_seats CHECK (seats BETWEEN 1 AND 20),
    CONSTRAINT chk_restaurant_tables_status
        CHECK (status IN ('Available', 'Reserved', 'Occupied'))
);

CREATE INDEX idx_tables_public_code ON restaurant_tables (public_code);
CREATE INDEX idx_tables_user ON restaurant_tables (user_id);
CREATE INDEX idx_tables_active ON restaurant_tables (active);
CREATE INDEX idx_tables_status ON restaurant_tables (status);
CREATE INDEX idx_tables_seats ON restaurant_tables (seats);

-- =========================
-- ORDERS
-- =========================
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_code VARCHAR(20) NOT NULL,
    type_order VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    phone VARCHAR(20),
    notes TEXT,
    delivery_address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    table_id UUID,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_orders_public_code UNIQUE (public_code),
    CONSTRAINT fk_orders_table
        FOREIGN KEY (table_id) REFERENCES restaurant_tables (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_type_order
        CHECK (type_order IN ('DINE_IN', 'DELIVERY')),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_orders_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED'))
);

CREATE INDEX idx_orders_public_code ON orders (public_code);
CREATE INDEX idx_orders_type ON orders (type_order);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_table ON orders (table_id);
CREATE INDEX idx_orders_created_by ON orders (created_by);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- =========================
-- ORDER ITEMS
-- =========================
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quantity INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu_item
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_quantity CHECK (quantity >= 1)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_menu_item ON order_items (menu_item_id);
CREATE INDEX idx_order_items_created_at ON order_items (created_at);

-- =========================
-- RESERVATIONS
-- =========================
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_code VARCHAR(20) NOT NULL,
    number_of_people INTEGER NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    email_customer VARCHAR(255) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    duration_reservation_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason TEXT,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_reservations_code UNIQUE (reservation_code),
    CONSTRAINT uq_reservations_email UNIQUE (email_customer),
    CONSTRAINT fk_reservations_created_by
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reservations_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_reservations_people CHECK (number_of_people BETWEEN 1 AND 50),
    CONSTRAINT chk_reservations_duration CHECK (duration_reservation_minutes >= 1),
    CONSTRAINT chk_reservations_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'NO_SHOW', 'COMPLETED'))
);

CREATE INDEX idx_reservations_start_at ON reservations (start_at);
CREATE INDEX idx_reservations_end_at ON reservations (end_at);
CREATE INDEX idx_reservations_customer_phone ON reservations (customer_phone);
CREATE INDEX idx_reservations_created_by ON reservations (created_by);
CREATE INDEX idx_reservations_status ON reservations (status);
CREATE INDEX idx_reservations_code ON reservations (reservation_code);

-- =========================
-- RESERVATION TABLE LINK
-- =========================
CREATE TABLE reservation_tables (
    reservation_id UUID NOT NULL,
    table_id UUID NOT NULL,
    PRIMARY KEY (reservation_id, table_id),
    CONSTRAINT fk_reservation_tables_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_tables_table
        FOREIGN KEY (table_id) REFERENCES restaurant_tables (id) ON DELETE CASCADE
);

CREATE INDEX idx_reservation_tables_table_id ON reservation_tables (table_id);

-- =========================
-- RESERVATION REQUESTS
-- =========================
CREATE TABLE reservation_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reservation_id UUID NOT NULL,
    user_id UUID,
    CONSTRAINT fk_reservation_request_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_request_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_reservation_request_status
        CHECK (status IN ('ACCEPTED', 'REFUSED', 'PENDING'))
);

CREATE INDEX idx_reservation_request_reservation ON reservation_request (reservation_id);
CREATE INDEX idx_reservation_request_user ON reservation_request (user_id);
CREATE INDEX idx_reservation_request_status ON reservation_request (status);
CREATE INDEX idx_reservation_request_created_at ON reservation_request (created_at);

-- =========================
-- PAYMENTS
-- =========================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(120),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_transaction_reference UNIQUE (transaction_reference),
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'PAID', 'FAILED'))
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);
