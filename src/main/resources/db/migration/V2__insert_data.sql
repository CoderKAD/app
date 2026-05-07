

-- USERS
INSERT INTO users (id, user_name, password_hash, email, roles, created_at, updated_at) VALUES
('aaaaaaaa-bbbb-4ccc-8ddd-000000000001', 'admin.branch2', '$2a$10$JY4oVi/I6gNx3OQZ5evleO0AuS9FDBrxHDZzZClR5brsBKAtcn2yy', 'admin.branch2@restaurant.local', 'ADMIN', TIMESTAMP '2026-05-07 13:00:00', TIMESTAMP '2026-05-07 13:00:00'),
('aaaaaaaa-bbbb-4ccc-8ddd-000000000002', 'cashier.branch2', '$2a$10$JY4oVi/I6gNx3OQZ5evleO0AuS9FDBrxHDZzZClR5brsBKAtcn2yy', 'cashier.branch2@restaurant.local', 'CASHIER', TIMESTAMP '2026-05-07 13:00:00', TIMESTAMP '2026-05-07 13:00:00'),
('aaaaaaaa-bbbb-4ccc-8ddd-000000000003', 'kitchen.branch2', '$2a$10$JY4oVi/I6gNx3OQZ5evleO0AuS9FDBrxHDZzZClR5brsBKAtcn2yy', 'kitchen.branch2@restaurant.local', 'KITCHEN', TIMESTAMP '2026-05-07 13:00:00', TIMESTAMP '2026-05-07 13:00:00'),
('aaaaaaaa-bbbb-4ccc-8ddd-000000000004', 'delivery.branch2', '$2a$10$JY4oVi/I6gNx3OQZ5evleO0AuS9FDBrxHDZzZClR5brsBKAtcn2yy', 'delivery.branch2@restaurant.local', 'DELIVERY', TIMESTAMP '2026-05-07 13:00:00', TIMESTAMP '2026-05-07 13:00:00'),
('aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'customer.branch2', '$2a$10$JY4oVi/I6gNx3OQZ5evleO0AuS9FDBrxHDZzZClR5brsBKAtcn2yy', 'customer.branch2@restaurant.local', 'CUSTOMER', TIMESTAMP '2026-05-07 13:00:00', TIMESTAMP '2026-05-07 13:00:00');

-- STAFFS
INSERT INTO staffs (id, first_name, last_name, salary, position, date_joined, date_left, cin, created_at, updated_at, user_id) VALUES
('bbbbbbbb-0000-4000-8000-000000000001', 'Salma', 'Ait', 9800.00, 'Restaurant Manager', DATE '2024-06-01', NULL, 'EE000011', TIMESTAMP '2026-05-07 13:05:00', TIMESTAMP '2026-05-07 13:05:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001'),
('bbbbbbbb-0000-4000-8000-000000000002', 'Hassan', 'Aziz', 6400.00, 'Cashier', DATE '2024-06-02', NULL, 'FF000012', TIMESTAMP '2026-05-07 13:05:00', TIMESTAMP '2026-05-07 13:05:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000002'),
('bbbbbbbb-0000-4000-8000-000000000003', 'Meryem', 'Naji', 7300.00, 'Chef de Partie', DATE '2024-06-03', NULL, 'GG000013', TIMESTAMP '2026-05-07 13:05:00', TIMESTAMP '2026-05-07 13:05:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000003'),
('bbbbbbbb-0000-4000-8000-000000000004', 'Yahya', 'Soufiani', 5900.00, 'Delivery Rider', DATE '2024-06-04', NULL, 'HH000014', TIMESTAMP '2026-05-07 13:05:00', TIMESTAMP '2026-05-07 13:05:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000004');

-- CATEGORIES
INSERT INTO categories_menu (id, category_name, sort_order, active, created_at, updated_at) VALUES
('cccccccc-0000-4000-8000-000000000001', 'Seafood', 7, TRUE, TIMESTAMP '2026-05-07 13:10:00', TIMESTAMP '2026-05-07 13:10:00'),
('cccccccc-0000-4000-8000-000000000002', 'Grill', 8, TRUE, TIMESTAMP '2026-05-07 13:10:00', TIMESTAMP '2026-05-07 13:10:00'),
('cccccccc-0000-4000-8000-000000000003', 'Breakfast', 9, TRUE, TIMESTAMP '2026-05-07 13:10:00', TIMESTAMP '2026-05-07 13:10:00'),
('cccccccc-0000-4000-8000-000000000004', 'Specials', 10, TRUE, TIMESTAMP '2026-05-07 13:10:00', TIMESTAMP '2026-05-07 13:10:00');

-- MENU ITEMS
INSERT INTO menu_items (id, name, description, price, active, image_url, prep_station, created_at, updated_at, category_id) VALUES
('dddddddd-0000-4000-8000-000000000001', 'Grilled Salmon', 'Salmon fillet with lemon butter sauce.', 28.00, TRUE, NULL, 'Grill', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000001'),
('dddddddd-0000-4000-8000-000000000002', 'Shrimp Skewers', 'Shrimp skewers with herb marinade.', 22.50, TRUE, NULL, 'Seafood', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000001'),
('dddddddd-0000-4000-8000-000000000003', 'Mixed Grill', 'Chicken, beef and sausage on one plate.', 31.00, TRUE, NULL, 'Grill', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000002'),
('dddddddd-0000-4000-8000-000000000004', 'Beef Burger', 'Beef burger with fries and sauce.', 16.50, TRUE, NULL, 'Grill', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000002'),
('dddddddd-0000-4000-8000-000000000005', 'Breakfast Omelette', 'Egg omelette with cheese and herbs.', 9.00, TRUE, NULL, 'Kitchen', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000003'),
('dddddddd-0000-4000-8000-000000000006', 'Pancakes', 'Fluffy pancakes with maple syrup.', 11.00, TRUE, NULL, 'Pastry', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000003'),
('dddddddd-0000-4000-8000-000000000007', 'Chef Special Pasta', 'Pasta of the day with chef sauce.', 17.00, TRUE, NULL, 'Hot Kitchen', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000004'),
('dddddddd-0000-4000-8000-000000000008', 'Chocolate Mousse', 'Light chocolate mousse dessert.', 8.50, TRUE, NULL, 'Pastry', TIMESTAMP '2026-05-07 13:15:00', TIMESTAMP '2026-05-07 13:15:00', 'cccccccc-0000-4000-8000-000000000004');

-- RESTAURANT TABLES
INSERT INTO restaurant_tables (id, label, seats, public_code, active, status, created_at, updated_at, user_id) VALUES
('eeeeeeee-0000-4000-8000-000000000001', 'Table 7', 2, 'TAB-0007', TRUE, 'Available', TIMESTAMP '2026-05-07 13:20:00', TIMESTAMP '2026-05-07 13:20:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001'),
('eeeeeeee-0000-4000-8000-000000000002', 'Table 8', 4, 'TAB-0008', TRUE, 'Reserved', TIMESTAMP '2026-05-07 13:20:00', TIMESTAMP '2026-05-07 13:20:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000002'),
('eeeeeeee-0000-4000-8000-000000000003', 'Table 9', 6, 'TAB-0009', TRUE, 'Occupied', TIMESTAMP '2026-05-07 13:20:00', TIMESTAMP '2026-05-07 13:20:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000003'),
('eeeeeeee-0000-4000-8000-000000000004', 'Table 10', 8, 'TAB-0010', TRUE, 'Available', TIMESTAMP '2026-05-07 13:20:00', TIMESTAMP '2026-05-07 13:20:00', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000004');

-- ORDERS
INSERT INTO orders (id, public_code, type_order, status, payment_status, phone, notes, delivery_address, created_at, updated_at, table_id, created_by, updated_by) VALUES
('bbbbbbbb-1111-4111-8111-000000000001', 'ORD-0005', 'DINE_IN', 'PREPARING', 'PENDING', NULL, 'Lunch for table 7.', NULL, TIMESTAMP '2026-05-07 13:30:00', TIMESTAMP '2026-05-07 13:35:00', 'eeeeeeee-0000-4000-8000-000000000001', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000002'),
('bbbbbbbb-1111-4111-8111-000000000002', 'ORD-0006', 'DINE_IN', 'READY', 'PAID', NULL, 'Breakfast service.', NULL, TIMESTAMP '2026-05-07 13:32:00', TIMESTAMP '2026-05-07 13:38:00', 'eeeeeeee-0000-4000-8000-000000000002', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000002', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000003'),
('bbbbbbbb-1111-4111-8111-000000000003', 'ORD-0007', 'DELIVERY', 'CONFIRMED', 'PAID', '0655555555', 'Evening delivery.', '25 Avenue Yacoub El Mansour, Casablanca', TIMESTAMP '2026-05-07 13:34:00', TIMESTAMP '2026-05-07 13:40:00', NULL, 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000004'),
('bbbbbbbb-1111-4111-8111-000000000004', 'ORD-0008', 'DELIVERY', 'COMPLETED', 'PAID', '0666666666', 'Delivered to office.', '12 Boulevard Zerktouni, Rabat', TIMESTAMP '2026-05-07 13:36:00', TIMESTAMP '2026-05-07 13:50:00', NULL, 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000004');

-- ORDER ITEMS
INSERT INTO order_items (id, quantity, notes, created_at, updated_at, order_id, menu_item_id) VALUES
('ffffffff-0000-4000-8000-000000000001', 1, 'No sauce', TIMESTAMP '2026-05-07 13:31:00', TIMESTAMP '2026-05-07 13:31:00', 'bbbbbbbb-1111-4111-8111-000000000001', 'dddddddd-0000-4000-8000-000000000003'),
('ffffffff-0000-4000-8000-000000000002', 2, 'Extra cheese', TIMESTAMP '2026-05-07 13:31:30', TIMESTAMP '2026-05-07 13:31:30', 'bbbbbbbb-1111-4111-8111-000000000001', 'dddddddd-0000-4000-8000-000000000007'),
('ffffffff-0000-4000-8000-000000000003', 1, 'Hot please', TIMESTAMP '2026-05-07 13:33:00', TIMESTAMP '2026-05-07 13:33:00', 'bbbbbbbb-1111-4111-8111-000000000002', 'dddddddd-0000-4000-8000-000000000005'),
('ffffffff-0000-4000-8000-000000000004', 1, 'No nuts', TIMESTAMP '2026-05-07 13:33:30', TIMESTAMP '2026-05-07 13:33:30', 'bbbbbbbb-1111-4111-8111-000000000002', 'dddddddd-0000-4000-8000-000000000008'),
('ffffffff-0000-4000-8000-000000000005', 2, 'Sealed packaging', TIMESTAMP '2026-05-07 13:35:30', TIMESTAMP '2026-05-07 13:35:30', 'bbbbbbbb-1111-4111-8111-000000000003', 'dddddddd-0000-4000-8000-000000000001'),
('ffffffff-0000-4000-8000-000000000006', 1, 'Extra crispy', TIMESTAMP '2026-05-07 13:36:00', TIMESTAMP '2026-05-07 13:36:00', 'bbbbbbbb-1111-4111-8111-000000000004', 'dddddddd-0000-4000-8000-000000000004');

-- PAYMENTS
INSERT INTO payments (id, order_id, amount, status, payment_method, transaction_reference, paid_at, created_at, updated_at) VALUES
('99999999-0000-4000-8000-000000000001', 'bbbbbbbb-1111-4111-8111-000000000001', 48.00, 'PENDING', 'CARD', 'PAY-20260507-0005', NULL, TIMESTAMP '2026-05-07 13:36:00', TIMESTAMP '2026-05-07 13:36:00'),
('99999999-0000-4000-8000-000000000002', 'bbbbbbbb-1111-4111-8111-000000000002', 20.00, 'PAID', 'CASH', 'PAY-20260507-0006', TIMESTAMP '2026-05-07 13:39:00', TIMESTAMP '2026-05-07 13:39:00', TIMESTAMP '2026-05-07 13:39:00'),
('99999999-0000-4000-8000-000000000003', 'bbbbbbbb-1111-4111-8111-000000000003', 36.50, 'PAID', 'ONLINE', 'PAY-20260507-0007', TIMESTAMP '2026-05-07 13:41:00', TIMESTAMP '2026-05-07 13:41:00', TIMESTAMP '2026-05-07 13:41:00'),
('99999999-0000-4000-8000-000000000004', 'bbbbbbbb-1111-4111-8111-000000000004', 16.50, 'PAID', 'CARD', 'PAY-20260507-0008', TIMESTAMP '2026-05-07 13:52:00', TIMESTAMP '2026-05-07 13:52:00', TIMESTAMP '2026-05-07 13:52:00');

-- RESERVATIONS
INSERT INTO reservations (
    id,
    reservation_code,
    number_of_people,
    customer_name,
    customer_phone,
    email_customer,
    start_at,
    end_at,
    duration_reservation_minutes,
    status,
    notes,
    created_at,
    updated_at,
    confirmed_at,
    cancelled_at,
    cancel_reason,
    created_by,
    updated_by
) VALUES
('aaaaaaaa-1111-4000-8000-000000000001', 'RSV-0005', 2, 'Nour El Amrani', '0677777777', 'nour.amrani@restaurant.local', TIMESTAMP '2030-01-12 12:00:00', TIMESTAMP '2030-01-12 13:30:00', 90, 'CONFIRMED', 'Birthday lunch.', TIMESTAMP '2026-05-07 13:45:00', TIMESTAMP '2026-05-07 13:45:00', TIMESTAMP '2030-01-12 10:00:00', NULL, NULL, 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001'),
('aaaaaaaa-1111-4000-8000-000000000002', 'RSV-0006', 5, 'Othman Berrada', '0688888888', 'othman.berrada@restaurant.local', TIMESTAMP '2030-01-12 17:00:00', TIMESTAMP '2030-01-12 19:00:00', 120, 'PENDING', 'Family dinner.', TIMESTAMP '2026-05-07 13:46:00', TIMESTAMP '2026-05-07 13:46:00', NULL, NULL, NULL, 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005'),
('aaaaaaaa-1111-4000-8000-000000000003', 'RSV-0007', 4, 'Lina El Idrissi', '0699999999', 'lina.elidrissi@restaurant.local', TIMESTAMP '2030-01-13 12:00:00', TIMESTAMP '2030-01-13 14:00:00', 120, 'CANCELLED', 'Change of plans.', TIMESTAMP '2026-05-07 13:47:00', TIMESTAMP '2026-05-07 13:47:00', NULL, TIMESTAMP '2030-01-12 16:00:00', 'Change of plans.', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001'),
('aaaaaaaa-1111-4000-8000-000000000004', 'RSV-0008', 6, 'Adil Hakam', '0601010101', 'adil.hakam@restaurant.local', TIMESTAMP '2030-01-13 18:00:00', TIMESTAMP '2030-01-13 21:00:00', 180, 'COMPLETED', 'Corporate dinner.', TIMESTAMP '2026-05-07 13:48:00', TIMESTAMP '2026-05-07 13:48:00', TIMESTAMP '2030-01-13 15:00:00', NULL, NULL, 'aaaaaaaa-bbbb-4ccc-8ddd-000000000001', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000002');

INSERT INTO reservation_tables (reservation_id, table_id) VALUES
('aaaaaaaa-1111-4000-8000-000000000001', 'eeeeeeee-0000-4000-8000-000000000001'),
('aaaaaaaa-1111-4000-8000-000000000002', 'eeeeeeee-0000-4000-8000-000000000002'),
('aaaaaaaa-1111-4000-8000-000000000002', 'eeeeeeee-0000-4000-8000-000000000003'),
('aaaaaaaa-1111-4000-8000-000000000003', 'eeeeeeee-0000-4000-8000-000000000004'),
('aaaaaaaa-1111-4000-8000-000000000004', 'eeeeeeee-0000-4000-8000-000000000001'),
('aaaaaaaa-1111-4000-8000-000000000004', 'eeeeeeee-0000-4000-8000-000000000004');

INSERT INTO reservation_request (id, status, created_at, updated_at, reservation_id, user_id) VALUES
('bbbbbbbb-1111-4000-8000-000000000001', 'ACCEPTED', TIMESTAMP '2026-05-07 13:49:00', TIMESTAMP '2026-05-07 13:49:00', 'aaaaaaaa-1111-4000-8000-000000000001', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005'),
('bbbbbbbb-1111-4000-8000-000000000002', 'PENDING', TIMESTAMP '2026-05-07 13:50:00', TIMESTAMP '2026-05-07 13:50:00', 'aaaaaaaa-1111-4000-8000-000000000002', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005'),
('bbbbbbbb-1111-4000-8000-000000000003', 'REFUSED', TIMESTAMP '2026-05-07 13:51:00', TIMESTAMP '2026-05-07 13:51:00', 'aaaaaaaa-1111-4000-8000-000000000003', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005'),
('bbbbbbbb-1111-4000-8000-000000000004', 'ACCEPTED', TIMESTAMP '2026-05-07 13:52:00', TIMESTAMP '2026-05-07 13:52:00', 'aaaaaaaa-1111-4000-8000-000000000004', 'aaaaaaaa-bbbb-4ccc-8ddd-000000000005');


