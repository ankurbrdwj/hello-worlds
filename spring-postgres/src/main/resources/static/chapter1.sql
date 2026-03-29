-- ============================================================
-- CHAPTER 1: "Give me all X, but only if Y"
-- Pattern: Filter early, clarify scope first
-- Real world: "Show users who...", "Orders in last 30 days..."
-- ============================================================
SELECT p.client_id, p.model AS active_model, p.date AS since
FROM phone_log p
         INNER JOIN (
    SELECT client_id, MAX(date) AS latest_date
    FROM phone_log
    GROUP BY client_id
) latest ON p.client_id = latest.client_id AND p.date = latest.latest_date
WHERE p.model = 'iPhone 12';

select * from phone_log pl

-- ─────────────────────────────────────────
-- SETUP: Sample tables used in this chapter
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    user_id    SERIAL PRIMARY KEY,
    name       VARCHAR(100),
    email      VARCHAR(100),
    status     VARCHAR(20),   -- 'active', 'inactive', 'banned'
    created_at DATE
);

CREATE TABLE IF NOT EXISTS orders (
    order_id   SERIAL PRIMARY KEY,
    user_id    INT,
    amount     NUMERIC(10,2),
    status     VARCHAR(20),   -- 'pending', 'shipped', 'delivered', 'cancelled'
    order_date DATE
);

CREATE TABLE IF NOT EXISTS events (
    event_id   SERIAL PRIMARY KEY,
    user_id    INT,
    event_type VARCHAR(50),   -- 'login', 'purchase', 'signup', 'click'
    country    VARCHAR(50),
    event_date DATE
);

-- ─────────────────────────────────────────
-- DUMMY DATA
-- ─────────────────────────────────────────

INSERT INTO users (name, email, status, created_at) VALUES
('Alice',   'alice@mail.com',   'active',   '2023-01-15'),
('Bob',     'bob@mail.com',     'inactive', '2022-06-10'),
('Charlie', 'charlie@mail.com', 'active',   '2024-03-22'),
('Diana',   'diana@mail.com',   'banned',   '2021-09-05'),
('Eve',     'eve@mail.com',     'active',   '2023-11-30'),
('Frank',   'frank@mail.com',   'inactive', '2020-04-18'),
('Grace',   'grace@mail.com',   'active',   '2024-07-01');

INSERT INTO orders (user_id, amount, status, order_date) VALUES
(1, 120.00, 'delivered',  '2026-01-10'),
(1, 340.50, 'shipped',    '2026-02-20'),
(2,  89.99, 'cancelled',  '2025-12-05'),
(3, 560.00, 'delivered',  '2026-03-01'),
(3,  45.00, 'pending',    '2026-03-15'),
(5, 230.00, 'delivered',  '2026-02-28'),
(5,  15.00, 'cancelled',  '2026-01-22'),
(7, 999.00, 'shipped',    '2026-03-10'),
(7,  75.00, 'pending',    '2026-03-18');

INSERT INTO events (user_id, event_type, country, event_date) VALUES
(1, 'login',    'US',  '2026-03-01'),
(1, 'purchase', 'US',  '2026-03-05'),
(2, 'login',    'UK',  '2026-02-10'),
(3, 'signup',   'IN',  '2026-01-20'),
(3, 'purchase', 'IN',  '2026-03-12'),
(4, 'login',    'US',  '2026-03-15'),
(5, 'click',    'CA',  '2026-03-19'),
(5, 'purchase', 'CA',  '2026-03-20'),
(7, 'login',    'AU',  '2026-03-18');


-- ─────────────────────────────────────────
-- EXAMPLE 1: Show me all ACTIVE users
-- Clarify: what does "active" mean? → status column
-- ─────────────────────────────────────────

SELECT user_id, name, email
FROM users
WHERE status = 'active';


-- ─────────────────────────────────────────
-- EXAMPLE 2: Orders in the last 30 days
-- Clarify: from today's date, what statuses count?
-- ─────────────────────────────────────────

SELECT order_id, user_id, amount, status, order_date
FROM orders
WHERE order_date >= CURRENT_DATE - INTERVAL '30 days';


-- ─────────────────────────────────────────
-- EXAMPLE 3: Active users who ordered in last 30 days
-- Combine: row filter (status) + time window filter
-- ─────────────────────────────────────────

SELECT DISTINCT u.user_id, u.name, u.email
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
WHERE
    u.status     = 'active'
    AND o.order_date >= CURRENT_DATE - INTERVAL '30 days';


-- ─────────────────────────────────────────
-- EXAMPLE 4: Orders delivered in last 30 days with amount > 100
-- Clarify: status = delivered, time window, amount threshold
-- ─────────────────────────────────────────

SELECT order_id, user_id, amount, order_date
FROM orders
WHERE
    status     = 'delivered'
    AND amount     > 100
    AND order_date >= CURRENT_DATE - INTERVAL '30 days';


-- ─────────────────────────────────────────
-- EXAMPLE 5: Events matching — purchases from US or CA
-- Clarify: event_type scope + country scope
-- ─────────────────────────────────────────

SELECT event_id, user_id, event_type, country, event_date
FROM events
WHERE
    event_type = 'purchase'
    AND country    IN ('US', 'CA');


-- ─────────────────────────────────────────
-- EXAMPLE 6: Users who signed up this year but never ordered
-- Clarify: "never ordered" = no row in orders table
-- ─────────────────────────────────────────

SELECT u.user_id, u.name, u.created_at
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id
WHERE
    u.created_at >= DATE_TRUNC('year', CURRENT_DATE)
    AND o.order_id IS NULL;


-- ─────────────────────────────────────────
-- EXAMPLE 7: Active users with more than 1 order in last 30 days
-- Clarify: "more than 1" = COUNT > 1, filter AFTER grouping → HAVING
-- ─────────────────────────────────────────

SELECT u.user_id, u.name, COUNT(o.order_id) AS order_count
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
WHERE
    u.status     = 'active'
    AND o.order_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY u.user_id, u.name
HAVING COUNT(o.order_id) > 1;


-- ─────────────────────────────────────────
-- PATTERN SUMMARY
-- ─────────────────────────────────────────
--
--  SELECT ...
--  FROM x
--  [JOIN y ON ...]
--  WHERE
--      <row_filter>          -- status, type, category
--      AND <time_window>     -- date >= CURRENT_DATE - INTERVAL '...'
--      AND <value_filter>    -- amount > 100, country IN (...)
--  [GROUP BY ...]
--  [HAVING <group_filter>];  -- COUNT > 1, SUM > X
--
--  RULE: Filter as early as possible → smaller rows for JOIN/GROUP BY
--  RULE: Use HAVING only when filtering on aggregates (COUNT, SUM, AVG)
--  RULE: Use LEFT JOIN + IS NULL for "never did X" patterns