Pre-aggregating
You’ll see: “total paid per user”, “revenue by country”, “counts after joins”

What to do when you see it: identify the “many” side and pre-aggregate it down to the join key before joining.

SQL

WITH b_agg AS (
    SELECT
        key,
        SUM(metric) AS metric
    FROM b
    WHERE <filters>
    GROUP BY key
)
SELECT
    a.key,
    COALESCE(SUM(b_agg.metric), 0) AS metric
FROM a
         LEFT JOIN b_agg
                   ON b_agg.key = a.key
GROUP BY a.key;  Tip
If you join two “many” tables to the same base, totals will multiply unless you pre-agg.

-- ============================================
-- 1. Total paid per user (classic)
-- ============================================
WITH payments_agg AS (
    SELECT user_id,
           SUM(amount) AS total_paid
    FROM payments
    WHERE status = 'SUCCESS'
    GROUP BY user_id
)
SELECT u.id,
       COALESCE(p.total_paid, 0) AS total_paid
FROM users u
         LEFT JOIN payments_agg p
                   ON p.user_id = u.id;

-- ============================================
-- 2. Revenue by country
-- ============================================
WITH orders_agg AS (
    SELECT customer_id,
           SUM(amount) AS revenue
    FROM orders
    GROUP BY customer_id
)
SELECT c.country,
       SUM(o.revenue) AS total_revenue
FROM customers c
         LEFT JOIN orders_agg o
                   ON o.customer_id = c.id
GROUP BY c.country;

-- ============================================
-- 3. Avoid double counting (orders + payments)
-- ============================================
-- ❌ WRONG (will multiply rows)
-- SELECT u.id, SUM(o.amount), SUM(p.amount)
-- FROM users u
-- JOIN orders o ON o.user_id = u.id
-- JOIN payments p ON p.user_id = u.id
-- GROUP BY u.id;

-- ✅ CORRECT (pre-aggregate both sides)
WITH orders_agg AS (
    SELECT user_id,
           SUM(amount) AS total_orders
    FROM orders
    GROUP BY user_id
),
     payments_agg AS (
         SELECT user_id,
                SUM(amount) AS total_payments
         FROM payments
         GROUP BY user_id
     )
SELECT u.id,
       COALESCE(o.total_orders, 0) AS total_orders,
       COALESCE(p.total_payments, 0) AS total_payments
FROM users u
         LEFT JOIN orders_agg o ON o.user_id = u.id
         LEFT JOIN payments_agg p ON p.user_id = u.id;

-- ============================================
-- 4. Count of orders per product category
-- ============================================
WITH order_items_agg AS (
    SELECT product_id,
           COUNT(*) AS order_count
    FROM order_items
    GROUP BY product_id
)
SELECT p.category,
       SUM(o.order_count) AS total_orders
FROM products p
         LEFT JOIN order_items_agg o
                   ON o.product_id = p.id
GROUP BY p.category;

-- ============================================
-- 5. Active users with total login count
-- ============================================
WITH login_agg AS (
    SELECT user_id,
           COUNT(*) AS login_count
    FROM logins
    GROUP BY user_id
)
SELECT u.id,
       u.status,
       COALESCE(l.login_count, 0) AS login_count
FROM users u
         LEFT JOIN login_agg l
                   ON l.user_id = u.id
WHERE u.status = 'ACTIVE';

-- ============================================
-- 6. Clients with total phone changes
-- ============================================
WITH phone_changes AS (
    SELECT client_id,
           COUNT(*) - 1 AS change_count
    FROM phone_log
    GROUP BY client_id
)
SELECT c.client_id,
       COALESCE(p.change_count, 0) AS changes
FROM clients c
         LEFT JOIN phone_changes p
                   ON p.client_id = c.client_id;

-- ============================================
-- 7. Revenue per day (filtered before aggregation)
-- ============================================
WITH filtered_orders AS (
    SELECT *
    FROM orders
    WHERE status = 'COMPLETED'
),
     daily_agg AS (
         SELECT order_date,
                SUM(amount) AS daily_revenue
         FROM filtered_orders
         GROUP BY order_date
     )
SELECT *
FROM daily_agg;

-- ============================================
-- 8. Users with both order count and avg order value
-- ============================================
WITH order_stats AS (
    SELECT user_id,
           COUNT(*) AS order_count,
           AVG(amount) AS avg_order
    FROM orders
    GROUP BY user_id
)
SELECT u.id,
       o.order_count,
       o.avg_order
FROM users u
         LEFT JOIN order_stats o
                   ON o.user_id = u.id;

-- ============================================
-- 9. Products with total sales (excluding returns)
-- ============================================
WITH sales_agg AS (
    SELECT product_id,
           SUM(amount) AS total_sales
    FROM order_items
    WHERE is_return = false
    GROUP BY product_id
)
SELECT p.id,
       p.name,
       COALESCE(s.total_sales, 0) AS total_sales
FROM products p
         LEFT JOIN sales_agg s
                   ON s.product_id = p.id;

-- ============================================
-- 10. Multi-join explosion example (important)
-- ============================================
-- Scenario: users + orders + logins
-- ❌ WRONG (cartesian multiplication)
-- orders (3) × logins (5) = 15 rows → wrong totals

-- ✅ CORRECT
WITH orders_agg AS (
    SELECT user_id,
           COUNT(*) AS order_count
    FROM orders
    GROUP BY user_id
),
     logins_agg AS (
         SELECT user_id,
                COUNT(*) AS login_count
         FROM logins
         GROUP BY user_id
     )
SELECT u.id,
       COALESCE(o.order_count, 0) AS order_count,
       COALESCE(l.login_count, 0) AS login_count
FROM users u
         LEFT JOIN orders_agg o ON o.user_id = u.id
         LEFT JOIN logins_agg l ON l.user_id = u.id;