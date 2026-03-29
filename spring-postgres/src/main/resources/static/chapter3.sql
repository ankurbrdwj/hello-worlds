-- ============================================
-- 1. Customers with at least 3 orders
-- ============================================
SELECT customer_id,
       COUNT(*) AS order_count
FROM orders
GROUP BY customer_id
HAVING COUNT(*) >= 3;

-- ============================================
-- 2. Products with average price > 100
-- ============================================
SELECT product_id,
       AVG(price) AS avg_price
FROM order_items
GROUP BY product_id
HAVING AVG(price) > 100;

-- ============================================
-- 3. Users who logged in on at least 5 distinct days
-- ============================================
SELECT user_id,
       COUNT(DISTINCT login_date) AS active_days
FROM logins
GROUP BY user_id
HAVING COUNT(DISTINCT login_date) >= 5;

-- ============================================
-- 4. Customers whose total spend > 5000
-- ============================================
SELECT customer_id,
       SUM(amount) AS total_spent
FROM orders
GROUP BY customer_id
HAVING SUM(amount) > 5000;

-- ============================================
-- 5. Employees managing at least 2 people
-- ============================================
SELECT manager_id,
       COUNT(*) AS report_count
FROM employees
WHERE manager_id IS NOT NULL
GROUP BY manager_id
HAVING COUNT(*) >= 2;

-- ============================================
-- 6. Weeks with at least 3 orders (time-based grouping)
-- ============================================
SELECT DATE_TRUNC('week', order_date) AS week,
       COUNT(*) AS order_count
FROM orders
GROUP BY week
HAVING COUNT(*) >= 3;

-- ============================================
-- 7. Customers who bought at least 2 different products
-- ============================================
SELECT customer_id,
       COUNT(DISTINCT product_id) AS distinct_products
FROM orders
GROUP BY customer_id
HAVING COUNT(DISTINCT product_id) >= 2;

-- ============================================
-- 8. Complex metric: avg order value > 200 AND count >= 3
-- (Using CTE for clarity)
-- ============================================
WITH per_customer AS (
    SELECT customer_id,
           COUNT(*) AS order_count,
           AVG(amount) AS avg_order_value
    FROM orders
    GROUP BY customer_id
)
SELECT *
FROM per_customer
WHERE order_count >= 3
  AND avg_order_value > 200;

-- ============================================
-- 9. Clients whose latest phone changes >= 2 times
-- (count transitions)
-- ============================================
WITH changes AS (
    SELECT client_id,
           COUNT(*) - 1 AS change_count
    FROM phone_log
    GROUP BY client_id
)
SELECT *
FROM changes
WHERE change_count >= 2;

-- ============================================
-- 10. Top spenders (top 10% by total spend)
-- ============================================
WITH totals AS (
    SELECT customer_id,
           SUM(amount) AS total_spent
    FROM orders
    GROUP BY customer_id
),
     ranked AS (
         SELECT *,
                NTILE(10) OVER (ORDER BY total_spent DESC) AS bucket
         FROM totals
     )
SELECT *
FROM ranked
WHERE bucket = 1;

-- ============================================
-- 11. Duplicate detection: emails used >= 2 times
-- ============================================
SELECT email,
       COUNT(*) AS cnt
FROM users
GROUP BY email
HAVING COUNT(*) >= 2;

-- ============================================
-- 12. Orders per day > average daily orders
-- ============================================
WITH daily AS (
    SELECT order_date,
           COUNT(*) AS daily_orders
    FROM orders
    GROUP BY order_date
),
     avg_val AS (
         SELECT AVG(daily_orders) AS avg_orders FROM daily
     )
SELECT d.*
FROM daily d
         CROSS JOIN avg_val a
WHERE d.daily_orders > a.avg_orders;