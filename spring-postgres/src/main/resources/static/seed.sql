-- ============================================================
-- seed.sql — Common tables + data for chapters 1–11
-- Run this once against your local PostgreSQL to be able to
-- execute every query in every chapter file.
-- ============================================================

-- ============================================================
-- DROP (reverse dependency order) — always start fresh
-- ============================================================
DROP TABLE IF EXISTS MovieRating       CASCADE;
DROP TABLE IF EXISTS Confirmations     CASCADE;
DROP TABLE IF EXISTS Signups           CASCADE;
DROP TABLE IF EXISTS LeetUsers         CASCADE;
DROP TABLE IF EXISTS Movies            CASCADE;
DROP TABLE IF EXISTS Stocks            CASCADE;
DROP TABLE IF EXISTS Logs              CASCADE;
DROP TABLE IF EXISTS Employee          CASCADE;
DROP TABLE IF EXISTS Department        CASCADE;
DROP TABLE IF EXISTS Accounts          CASCADE;
DROP TABLE IF EXISTS Customer          CASCADE;
DROP TABLE IF EXISTS Stadium           CASCADE;
DROP TABLE IF EXISTS Weather           CASCADE;
DROP TABLE IF EXISTS Queries           CASCADE;

DROP TABLE IF EXISTS daily_metrics     CASCADE;
DROP TABLE IF EXISTS stock_prices      CASCADE;
DROP TABLE IF EXISTS user_status_log   CASCADE;
DROP TABLE IF EXISTS user_scores       CASCADE;
DROP TABLE IF EXISTS user_events       CASCADE;
DROP TABLE IF EXISTS referrals         CASCADE;
DROP TABLE IF EXISTS subscriptions     CASCADE;
DROP TABLE IF EXISTS leaderboard       CASCADE;
DROP TABLE IF EXISTS campaign_responses CASCADE;
DROP TABLE IF EXISTS impressions       CASCADE;
DROP TABLE IF EXISTS sessions          CASCADE;
DROP TABLE IF EXISTS events            CASCADE;
DROP TABLE IF EXISTS logins            CASCADE;
DROP TABLE IF EXISTS phone_log         CASCADE;
DROP TABLE IF EXISTS clients           CASCADE;
DROP TABLE IF EXISTS employees         CASCADE;
DROP TABLE IF EXISTS payments          CASCADE;
DROP TABLE IF EXISTS order_items       CASCADE;
DROP TABLE IF EXISTS orders            CASCADE;
DROP TABLE IF EXISTS products          CASCADE;
DROP TABLE IF EXISTS customers         CASCADE;
DROP TABLE IF EXISTS users             CASCADE;

-- ============================================================
-- SECTION 1: Core business tables
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100),
    email      VARCHAR(100),
    status     VARCHAR(20),   -- 'active' | 'inactive' | 'banned'
    created_at DATE
);

-- ch1 references user_id; ch2+ references id.
-- Both work because id IS the primary key — just alias as needed.

CREATE TABLE IF NOT EXISTS customers (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(100),
    country VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS products (
    id       SERIAL PRIMARY KEY,
    name     VARCHAR(100),
    category VARCHAR(50),
    price    NUMERIC(10,2)
);

CREATE TABLE IF NOT EXISTS orders (
    id          SERIAL PRIMARY KEY,
    user_id     INT REFERENCES users(id),
    customer_id INT REFERENCES customers(id),
    product_id  INT REFERENCES products(id),
    product     VARCHAR(100),          -- ch2 uses a raw string column
    amount      NUMERIC(10,2),
    status      VARCHAR(20),           -- 'pending'|'shipped'|'delivered'|'cancelled'|'COMPLETED'
    created_at  TIMESTAMP DEFAULT NOW(),
    order_date  DATE
);

CREATE TABLE IF NOT EXISTS order_items (
    id         SERIAL PRIMARY KEY,
    order_id   INT REFERENCES orders(id),
    product_id INT REFERENCES products(id),
    amount     NUMERIC(10,2),
    price      NUMERIC(10,2),
    is_return  BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS payments (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    order_id   INT REFERENCES orders(id),
    amount     NUMERIC(10,2),
    status     VARCHAR(20),   -- 'SUCCESS' | 'FAILED' | 'PENDING'
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS employees (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100),
    manager_id INT,           -- self-ref; nullable for top-level managers
    salary     NUMERIC(10,2)
);

CREATE TABLE IF NOT EXISTS clients (
    client_id SERIAL PRIMARY KEY,
    name      VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS phone_log (
    id        SERIAL PRIMARY KEY,
    client_id INT REFERENCES clients(client_id),
    model     VARCHAR(100),
    date      DATE
);

CREATE TABLE IF NOT EXISTS logins (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    login_date DATE,
    device     VARCHAR(50)    -- 'mobile' | 'desktop' | 'tablet'
);

CREATE TABLE IF NOT EXISTS events (
    id          SERIAL PRIMARY KEY,
    user_id     INT REFERENCES users(id),
    event_name  VARCHAR(50),   -- 'signup' | 'verify' | 'purchase'  (ch6)
    event_type  VARCHAR(50),   -- 'login'  | 'purchase' | 'click'   (ch1)
    country     VARCHAR(50),
    created_at  TIMESTAMP DEFAULT NOW(),
    event_date  DATE,
    converted   BOOLEAN DEFAULT FALSE,
    campaign_id INT
);

-- ============================================================
-- SECTION 2: Analytics tables
-- ============================================================

CREATE TABLE IF NOT EXISTS sessions (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    session_id VARCHAR(50),
    device     VARCHAR(20),   -- 'mobile' | 'desktop' | 'tablet'
    revenue    NUMERIC(10,2) DEFAULT 0,
    started_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    week       INT            -- ISO week number, used in ch9 rolling examples
);

CREATE TABLE IF NOT EXISTS impressions (
    id      SERIAL PRIMARY KEY,
    ad_id   INT,
    user_id INT REFERENCES users(id),
    clicked BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS campaign_responses (
    id          SERIAL PRIMARY KEY,
    campaign_id INT,
    completed   BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS leaderboard (
    user_id INT REFERENCES users(id),
    score   INT
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    plan       VARCHAR(20),   -- 'free' | 'paid'
    started_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS referrals (
    referrer_id INT REFERENCES users(id),
    referred_id INT REFERENCES users(id),
    PRIMARY KEY (referrer_id, referred_id)
);

CREATE TABLE IF NOT EXISTS user_events (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    event_date DATE,
    event_type VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS user_scores (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    event_date DATE,
    score      INT
);

CREATE TABLE IF NOT EXISTS user_status_log (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    event_date DATE,
    status     VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS stock_prices (
    id         SERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    price      NUMERIC(10,2)
);

CREATE TABLE IF NOT EXISTS daily_metrics (
    dt     DATE PRIMARY KEY,
    metric NUMERIC(10,2)
);

-- ============================================================
-- SECTION 3: LeetCode tables
-- ============================================================

CREATE TABLE IF NOT EXISTS Department (
    id   INT PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS Employee (
    id           INT PRIMARY KEY,
    name         VARCHAR(50),
    salary       INT,
    departmentId INT REFERENCES Department(id),
    managerId    INT            -- self-ref; nullable for CEO
);

CREATE TABLE IF NOT EXISTS Logs (
    id  INT PRIMARY KEY,
    num INT
);

CREATE TABLE IF NOT EXISTS Stocks (
    stock_name VARCHAR(50),
    operation  VARCHAR(10),   -- 'Buy' | 'Sell'
    price      INT
);

CREATE TABLE IF NOT EXISTS Movies (
    movie_id INT PRIMARY KEY,
    title    VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS LeetUsers (
    user_id INT PRIMARY KEY,
    name    VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS MovieRating (
    user_id    INT REFERENCES LeetUsers(user_id),
    movie_id   INT REFERENCES Movies(movie_id),
    rating     INT,
    created_at DATE,
    PRIMARY KEY (user_id, movie_id, created_at)
);

CREATE TABLE IF NOT EXISTS Queries (
    id          SERIAL PRIMARY KEY,
    query_name  VARCHAR(100),
    result      VARCHAR(100),
    position    INT,
    rating      INT
);

CREATE TABLE IF NOT EXISTS Signups (
    user_id    INT PRIMARY KEY,
    time_stamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Confirmations (
    user_id    INT REFERENCES Signups(user_id),
    time_stamp TIMESTAMP,
    action     VARCHAR(20),   -- 'confirmed' | 'timeout'
    PRIMARY KEY (user_id, time_stamp)
);

CREATE TABLE IF NOT EXISTS Accounts (
    account_id INT PRIMARY KEY,
    income     INT
);

CREATE TABLE IF NOT EXISTS Customer (
    customer_id  INT PRIMARY KEY,
    name         VARCHAR(50),
    visited_on   DATE,
    amount       INT
);

CREATE TABLE IF NOT EXISTS Stadium (
    id         INT PRIMARY KEY,
    visit_date DATE,
    people     INT
);

CREATE TABLE IF NOT EXISTS Weather (
    id          INT PRIMARY KEY,
    recordDate  DATE,
    temperature INT
);

-- ============================================================
-- SECTION 4: INSERT DATA
-- ============================================================

-- users (7 rows)
INSERT INTO users (name, email, status, created_at) VALUES
('Alice',   'alice@mail.com',   'active',   '2023-01-15'),
('Bob',     'bob@mail.com',     'inactive', '2022-06-10'),
('Charlie', 'charlie@mail.com', 'active',   '2024-03-22'),
('Diana',   'diana@mail.com',   'banned',   '2021-09-05'),
('Eve',     'eve@mail.com',     'active',   '2023-11-30'),
('Frank',   'frank@mail.com',   'inactive', '2020-04-18'),
('Grace',   'grace@mail.com',   'active',   '2026-01-01')
ON CONFLICT DO NOTHING;

-- customers
INSERT INTO customers (name, country) VALUES
('Alice',   'US'),
('Bob',     'UK'),
('Charlie', 'IN'),
('Diana',   'US'),
('Eve',     'CA'),
('Frank',   'DE'),
('Grace',   'AU')
ON CONFLICT DO NOTHING;

-- products
INSERT INTO products (name, category, price) VALUES
('iPhone 12',    'Electronics', 799.00),
('MacBook Pro',  'Electronics', 1999.00),
('AirPods',      'Electronics',  149.00),
('Desk Chair',   'Furniture',    350.00),
('Standing Desk','Furniture',    650.00),
('Python Book',  'Books',         49.00),
('SQL Guide',    'Books',         39.00)
ON CONFLICT DO NOTHING;

-- orders
INSERT INTO orders (user_id, customer_id, product_id, product, amount, status, created_at, order_date) VALUES
(1, 1, 1, 'iPhone 12',    799.00, 'delivered', '2026-01-10', '2026-01-10'),
(1, 1, 2, 'MacBook Pro', 1999.00, 'shipped',   '2026-02-20', '2026-02-20'),
(2, 2, 3, 'AirPods',      149.00, 'cancelled', '2025-12-05', '2025-12-05'),
(3, 3, 1, 'iPhone 12',    799.00, 'delivered', '2026-03-01', '2026-03-01'),
(3, 3, 4, 'Desk Chair',   350.00, 'pending',   '2026-03-15', '2026-03-15'),
(3, 3, 6, 'Python Book',   49.00, 'delivered', '2026-03-20', '2026-03-20'),
(5, 5, 2, 'MacBook Pro', 1999.00, 'delivered', '2026-02-28', '2026-02-28'),
(5, 5, 7, 'SQL Guide',     39.00, 'cancelled', '2026-01-22', '2026-01-22'),
(7, 7, 5, 'Standing Desk',650.00, 'shipped',   '2026-03-10', '2026-03-10'),
(7, 7, 3, 'AirPods',      149.00, 'pending',   '2026-03-18', '2026-03-18'),
(1, 1, 6, 'Python Book',   49.00, 'delivered', '2025-11-01', '2025-11-01'),
(4, 4, 1, 'iPhone 12',    799.00, 'delivered', '2025-10-05', '2025-10-05'),
(6, 6, 7, 'SQL Guide',     39.00, 'shipped',   '2026-04-01', '2026-04-01')
ON CONFLICT DO NOTHING;

-- order_items
INSERT INTO order_items (order_id, product_id, amount, price, is_return) VALUES
(1,  1, 1, 799.00, FALSE),
(2,  2, 1,1999.00, FALSE),
(3,  3, 1, 149.00, TRUE),
(4,  1, 1, 799.00, FALSE),
(5,  4, 1, 350.00, FALSE),
(6,  6, 1,  49.00, FALSE),
(7,  2, 1,1999.00, FALSE),
(8,  7, 1,  39.00, TRUE),
(9,  5, 1, 650.00, FALSE),
(10, 3, 1, 149.00, FALSE)
ON CONFLICT DO NOTHING;

-- payments
INSERT INTO payments (user_id, order_id, amount, status, created_at) VALUES
(1, 1,  799.00, 'SUCCESS', '2026-01-10'),
(1, 2, 1999.00, 'SUCCESS', '2026-02-20'),
(2, 3,  149.00, 'FAILED',  '2025-12-05'),
(3, 4,  799.00, 'SUCCESS', '2026-03-01'),
(5, 7, 1999.00, 'SUCCESS', '2026-02-28'),
(7, 9,  650.00, 'SUCCESS', '2026-03-10')
ON CONFLICT DO NOTHING;

-- employees (with manager hierarchy)
INSERT INTO employees (name, manager_id, salary) VALUES
('CEO',     NULL, 200000),  -- id=1
('VP Eng',  1,    150000),  -- id=2
('VP Sales',1,    140000),  -- id=3
('Alice',   2,     90000),  -- id=4
('Bob',     2,     85000),  -- id=5
('Charlie', 3,     80000),  -- id=6
('Diana',   3,     78000),  -- id=7
('Eve',     2,     88000)   -- id=8
ON CONFLICT DO NOTHING;

-- clients + phone_log (ch1, ch2, ch3, ch4)
INSERT INTO clients (name) VALUES
('Client A'), ('Client B'), ('Client C')
ON CONFLICT DO NOTHING;

INSERT INTO phone_log (client_id, model, date) VALUES
(1, 'iPhone 10', '2021-01-01'),
(1, 'iPhone 11', '2022-06-01'),
(1, 'iPhone 12', '2023-09-01'),
(2, 'iPhone 12', '2022-03-15'),
(3, 'Samsung S21', '2021-11-20'),
(3, 'iPhone 12',   '2023-05-10')
ON CONFLICT DO NOTHING;

-- logins (ch2, ch3)
INSERT INTO logins (user_id, login_date, device) VALUES
(1, '2026-01-01', 'mobile'),
(1, '2026-01-02', 'desktop'),
(1, '2026-01-03', 'mobile'),
(1, '2026-01-05', 'tablet'),
(1, '2026-01-07', 'mobile'),
(2, '2026-01-10', 'desktop'),
(3, '2026-01-01', 'mobile'),
(3, '2026-01-03', 'desktop'),
(3, '2026-01-05', 'mobile'),
(3, '2026-01-07', 'tablet'),
(3, '2026-01-09', 'mobile'),
(5, '2026-02-01', 'desktop'),
(5, '2026-02-10', 'mobile'),
(7, '2026-03-01', 'mobile'),
(7, '2026-03-01', 'desktop')   -- same user, two devices → triggers ch2 example 11
ON CONFLICT DO NOTHING;

-- events (ch1 event_type style + ch6 event_name / funnel style)
INSERT INTO events (user_id, event_name, event_type, country, created_at, event_date, converted, campaign_id) VALUES
(1, 'signup',   'signup',   'US', '2026-01-01', '2026-01-01', FALSE, 1),
(1, 'verify',   'login',    'US', '2026-01-02', '2026-01-02', FALSE, 1),
(1, 'purchase', 'purchase', 'US', '2026-01-05', '2026-01-05', TRUE,  1),
(2, 'signup',   'signup',   'UK', '2026-01-10', '2026-01-10', FALSE, 2),
(2, 'verify',   'login',    'UK', '2026-01-11', '2026-01-11', FALSE, 2),
(3, 'signup',   'signup',   'IN', '2026-01-20', '2026-01-20', FALSE, 1),
(3, 'purchase', 'purchase', 'IN', '2026-03-12', '2026-03-12', TRUE,  1),
(4, 'signup',   'login',    'US', '2026-03-15', '2026-03-15', FALSE, 2),
(5, 'signup',   'click',    'CA', '2026-03-19', '2026-03-19', FALSE, 2),
(5, 'purchase', 'purchase', 'CA', '2026-03-20', '2026-03-20', TRUE,  2),
(7, 'verify',   'login',    'AU', '2026-03-18', '2026-03-18', FALSE, 1)
ON CONFLICT DO NOTHING;

-- sessions (ch6 device, ch9 rolling, ch11 consecutive days)
INSERT INTO sessions (user_id, session_id, device, revenue, started_at, created_at, week) VALUES
(1, 'S001', 'mobile',  120.00, '2026-01-10 09:00', '2026-01-10 09:00', 2),
(1, 'S002', 'desktop', 340.00, '2026-01-11 14:00', '2026-01-11 14:00', 2),
(2, 'S003', 'mobile',   89.99, '2026-01-12 10:00', '2026-01-12 10:00', 2),
(3, 'S004', 'tablet',  560.00, '2026-01-13 08:00', '2026-01-13 08:00', 3),
(3, 'S005', 'mobile',   45.00, '2026-01-14 11:00', '2026-01-14 11:00', 3),
(5, 'S006', 'desktop', 230.00, '2026-01-14 15:00', '2026-01-14 15:00', 3),
(5, 'S007', 'mobile',   15.00, '2026-01-15 09:30', '2026-01-15 09:30', 3),
(7, 'S008', 'desktop', 999.00, '2026-01-15 16:00', '2026-01-15 16:00', 3),
(7, 'S009', 'tablet',   75.00, '2026-01-16 10:00', '2026-01-16 10:00', 3),
(1, 'S010', 'mobile',  200.00, '2026-02-01 09:00', '2026-02-01 09:00', 5),
(3, 'S011', 'desktop', 100.00, '2026-02-02 10:00', '2026-02-02 10:00', 5)
ON CONFLICT DO NOTHING;

-- impressions (ch7 CTR)
INSERT INTO impressions (ad_id, user_id, clicked) VALUES
(1, 1, TRUE),  (1, 2, FALSE), (1, 3, TRUE),
(1, 4, FALSE), (1, 5, TRUE),  (1, 6, FALSE),
(2, 1, FALSE), (2, 2, FALSE), (2, 3, FALSE),
(2, 4, TRUE),  (2, 5, FALSE), (2, 7, TRUE)
ON CONFLICT DO NOTHING;

-- campaign_responses (ch7 completion rate)
INSERT INTO campaign_responses (campaign_id, completed) VALUES
(1, TRUE), (1, TRUE), (1, FALSE), (1, TRUE),
(1, FALSE),(2, FALSE),(2, FALSE), (2, TRUE),
(3, TRUE), (3, TRUE), (3, TRUE),  (3, FALSE)
ON CONFLICT DO NOTHING;

-- leaderboard (ch8 RANK / DENSE_RANK)
INSERT INTO leaderboard (user_id, score) VALUES
(1, 9500), (2, 8800), (3, 8800),
(4, 7200), (5, 7200), (6, 6100), (7, 5000)
ON CONFLICT DO NOTHING;

-- subscriptions (ch6 free vs paid)
INSERT INTO subscriptions (user_id, plan, started_at) VALUES
(1, 'paid',  '2025-01-01'), (2, 'free',  '2025-03-01'),
(3, 'paid',  '2025-06-01'), (4, 'free',  '2024-12-01'),
(5, 'paid',  '2025-09-01'), (6, 'free',  '2025-11-01'),
(7, 'paid',  '2026-01-01')
ON CONFLICT DO NOTHING;

-- referrals (ch11 mutual referral)
INSERT INTO referrals (referrer_id, referred_id) VALUES
(1, 2), (2, 1),   -- mutual
(3, 4), (5, 6),
(1, 3)
ON CONFLICT DO NOTHING;

-- user_events (ch10 LAG, ch11 before/after)
INSERT INTO user_events (user_id, event_date, event_type) VALUES
(1, '2026-01-01', 'login'),    (1, '2026-01-03', 'purchase'),
(1, '2026-01-05', 'logout'),   (1, '2026-01-10', 'login'),
(2, '2026-01-02', 'login'),    (2, '2026-01-04', 'cancel'),
(3, '2026-01-01', 'signup'),   (3, '2026-01-02', 'login'),
(3, '2026-01-03', 'purchase'), (5, '2026-02-01', 'login'),
(5, '2026-02-02', 'purchase')
ON CONFLICT DO NOTHING;

-- user_scores (ch10 LAG delta)
INSERT INTO user_scores (user_id, event_date, score) VALUES
(1, '2026-01-01', 100), (1, '2026-01-08', 130),
(1, '2026-01-15', 120), (1, '2026-01-22', 160),
(2, '2026-01-01', 200), (2, '2026-01-08', 195),
(2, '2026-01-15', 210), (3, '2026-01-01', 80),
(3, '2026-01-08', 95),  (3, '2026-01-15', 110)
ON CONFLICT DO NOTHING;

-- user_status_log (ch10 state change detection)
INSERT INTO user_status_log (user_id, event_date, status) VALUES
(1, '2026-01-01', 'active'),   (1, '2026-02-01', 'active'),
(1, '2026-03-01', 'inactive'), (1, '2026-04-01', 'active'),
(2, '2026-01-01', 'active'),   (2, '2026-02-01', 'banned'),
(3, '2026-01-01', 'inactive'), (3, '2026-02-01', 'active')
ON CONFLICT DO NOTHING;

-- stock_prices (ch9 rolling 30-day high/low)
INSERT INTO stock_prices (created_at, price) VALUES
('2026-01-01', 150.00), ('2026-01-02', 153.00), ('2026-01-03', 148.50),
('2026-01-06', 155.00), ('2026-01-07', 160.00), ('2026-01-08', 158.00),
('2026-01-09', 162.00), ('2026-01-10', 159.00), ('2026-01-13', 165.00),
('2026-01-14', 163.00), ('2026-01-15', 170.00), ('2026-01-16', 168.00),
('2026-01-20', 172.00), ('2026-01-21', 175.00), ('2026-01-22', 173.00)
ON CONFLICT DO NOTHING;

-- daily_metrics (ch9 rolling avg header example)
INSERT INTO daily_metrics (dt, metric) VALUES
('2026-01-01', 100), ('2026-01-02', 120), ('2026-01-03', 95),
('2026-01-04', 110), ('2026-01-05', 130), ('2026-01-06', 115),
('2026-01-07', 140), ('2026-01-08', 125), ('2026-01-09', 135),
('2026-01-10', 150)
ON CONFLICT DO NOTHING;

-- ============================================================
-- LeetCode tables data
-- ============================================================

-- Department + Employee (ch5, ch8, ch11)
INSERT INTO Department (id, name) VALUES
(1, 'IT'), (2, 'Sales'), (3, 'HR')
ON CONFLICT DO NOTHING;

INSERT INTO Employee (id, name, salary, departmentId, managerId) VALUES
(1, 'Joe',   85000, 1, 4),
(2, 'Henry', 80000, 2, 5),
(3, 'Sam',   60000, 2, 5),
(4, 'Max',   90000, 1, NULL),
(5, 'Janet', 69000, 1, 4),
(6, 'Randy', 85000, 1, 4),
(7, 'Will',  70000, 1, 4),
(8, 'Mary',  95000, 2, NULL)
ON CONFLICT DO NOTHING;

-- Logs (ch8 consecutive numbers, ch10 LAG)
INSERT INTO Logs (id, num) VALUES
(1,1),(2,1),(3,1),(4,2),(5,2),(6,2),(7,3),(8,2),(9,2),(10,2)
ON CONFLICT DO NOTHING;

-- Stocks (ch6 capital gain/loss)
INSERT INTO Stocks (stock_name, operation, price) VALUES
('Leetcode', 'Buy',  1000), ('Leetcode', 'Sell', 1200),
('Leetcode', 'Buy',   900), ('Leetcode', 'Sell',  800),
('Handbag',  'Buy',   500), ('Handbag',  'Sell',  700),
('Corona',   'Buy',   600), ('Corona',   'Buy',   400),
('Corona',   'Sell',  900)
ON CONFLICT DO NOTHING;

-- Movies + LeetUsers + MovieRating (ch6)
INSERT INTO Movies (movie_id, title) VALUES
(1, 'Avengers'), (2, 'Frozen 2'), (3, 'Joker')
ON CONFLICT DO NOTHING;

INSERT INTO LeetUsers (user_id, name) VALUES
(1, 'Daniel'), (2, 'Monica'), (3, 'Maria'), (4, 'James')
ON CONFLICT DO NOTHING;

INSERT INTO MovieRating (user_id, movie_id, rating, created_at) VALUES
(1, 1, 3, '2020-01-12'), (1, 2, 4, '2020-02-11'),
(1, 3, 2, '2020-02-12'), (2, 1, 5, '2020-01-19'),
(2, 2, 4, '2020-02-01'), (3, 1, 3, '2020-02-22'),
(3, 2, 2, '2020-02-25'), (4, 1, 1, '2020-01-01')
ON CONFLICT DO NOTHING;

-- Queries (ch7 quality / poor percentage)
INSERT INTO Queries (query_name, result, position, rating) VALUES
('Dog', 'Golden Retriever', 1, 5), ('Dog', 'German Shepherd', 2, 5),
('Dog', 'Mule',             200, 1),('Cat', 'Shirazi',         5, 2),
('Cat', 'Siamese',           3, 3),('Cat', 'Sphynx',           7, 4)
ON CONFLICT DO NOTHING;

-- Signups + Confirmations (ch7 confirmation rate)
INSERT INTO Signups (user_id, time_stamp) VALUES
(3, '2020-03-21 10:16:13'), (7, '2020-01-04 13:57:59'),
(2, '2020-07-29 23:09:44'), (6, '2020-12-09 10:39:31')
ON CONFLICT DO NOTHING;

INSERT INTO Confirmations (user_id, time_stamp, action) VALUES
(3, '2021-01-06 03:30:46', 'timeout'),
(3, '2021-07-14 14:00:00', 'timeout'),
(7, '2021-06-12 11:57:29', 'confirmed'),
(7, '2021-06-13 12:58:28', 'confirmed'),
(2, '2021-01-22 00:00:00', 'confirmed'),
(2, '2021-02-28 23:59:59', 'timeout')
ON CONFLICT DO NOTHING;

-- Accounts (ch8 salary buckets)
INSERT INTO Accounts (account_id, income) VALUES
(3,  108939), (2,  12747), (8,  87709),
(6,  91796),  (1,   8875)
ON CONFLICT DO NOTHING;

-- Customer (ch9 LeetCode 1321 restaurant growth)
INSERT INTO Customer (customer_id, name, visited_on, amount) VALUES
(1, 'Jhon',    '2019-01-01', 100), (2, 'Daniel',  '2019-01-02', 110),
(3, 'Jade',    '2019-01-03', 120), (4, 'Khaled',  '2019-01-04', 130),
(5, 'Winston', '2019-01-05', 110), (6, 'Elvis',   '2019-01-06', 140),
(7, 'Anna',    '2019-01-07', 150), (8, 'Maria',   '2019-01-08', 80),
(9, 'Jaze',    '2019-01-09', 110), (1, 'Jhon',    '2019-01-10', 130),
(3, 'Jade',    '2019-01-10', 150)
ON CONFLICT DO NOTHING;

-- Stadium (ch11 LeetCode 601)
INSERT INTO Stadium (id, visit_date, people) VALUES
(1, '2017-01-01', 10),  (2, '2017-01-02', 109),
(3, '2017-01-03', 150), (4, '2017-01-04', 99),
(5, '2017-01-05', 145), (6, '2017-01-06', 1455),
(7, '2017-01-07', 199), (8, '2017-01-09', 188)
ON CONFLICT DO NOTHING;

-- Weather (ch10 LeetCode 197 rising temperature)
INSERT INTO Weather (id, recordDate, temperature) VALUES
(1, '2015-01-01', 10), (2, '2015-01-02', 25),
(3, '2015-01-03', 20), (4, '2015-01-04', 30)
ON CONFLICT DO NOTHING;