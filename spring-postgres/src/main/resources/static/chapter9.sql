Rolling metrics
You'll see: "7-day rolling average", "trailing 28 days", "cumulative revenue"

What to do when you see it: first compute the metric at the correct time grain (day/week/month), then apply a window frame.

SQL

SELECT
    dt,
    metric,
    AVG(metric) OVER (
        ORDER BY dt
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS rolling_7
FROM daily_metrics;

Tip
If dates are missing, "7 rows" ≠ "7 days". When gaps matter, fill them with a date spine first.

-- ============================================
-- Frame syntax reference
-- ============================================
-- ROWS  → counts physical rows (use when no date gaps are possible)
-- RANGE → counts logical values (use with INTERVAL when gaps may exist)
--
-- ROWS BETWEEN 6 PRECEDING AND CURRENT ROW   ← last 7 rows
-- ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW  ← cumulative (running total)
-- RANGE BETWEEN INTERVAL '6 days' PRECEDING AND CURRENT ROW  ← last 7 calendar days

-- ============================================
-- 1. 7-day rolling average (no gaps assumed)
-- ============================================
WITH daily AS (
    SELECT DATE(created_at) AS dt,
           SUM(amount)      AS revenue
    FROM orders
    GROUP BY DATE(created_at)
)
SELECT dt,
       revenue,
       AVG(revenue) OVER (
           ORDER BY dt
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ) AS rolling_7d_avg
FROM daily
ORDER BY dt;

-- ============================================
-- 2. 7-day rolling average (with date spine — gap-safe)
-- ============================================
-- Missing days get revenue = 0, so the average is over true calendar days
WITH spine AS (
    SELECT generate_series(
               MIN(DATE(created_at)),
               MAX(DATE(created_at)),
               INTERVAL '1 day'
           )::DATE AS dt
    FROM orders
),
daily AS (
    SELECT DATE(created_at) AS dt,
           SUM(amount)      AS revenue
    FROM orders
    GROUP BY DATE(created_at)
),
filled AS (
    SELECT s.dt,
           COALESCE(d.revenue, 0) AS revenue
    FROM spine s
             LEFT JOIN daily d ON d.dt = s.dt
)
SELECT dt,
       revenue,
       AVG(revenue) OVER (
           ORDER BY dt
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ) AS rolling_7d_avg
FROM filled
ORDER BY dt;

-- ============================================
-- 3. Cumulative (running) revenue
-- ============================================
WITH daily AS (
    SELECT DATE(created_at) AS dt,
           SUM(amount)      AS revenue
    FROM orders
    GROUP BY DATE(created_at)
)
SELECT dt,
       revenue,
       SUM(revenue) OVER (
           ORDER BY dt
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS cumulative_revenue
FROM daily
ORDER BY dt;

-- ============================================
-- 4. Cumulative revenue — reset each month
-- ============================================
WITH daily AS (
    SELECT DATE(created_at)                        AS dt,
           DATE_TRUNC('month', created_at)::DATE   AS month,
           SUM(amount)                             AS revenue
    FROM orders
    GROUP BY DATE(created_at), DATE_TRUNC('month', created_at)
)
SELECT dt,
       revenue,
       SUM(revenue) OVER (
           PARTITION BY month
           ORDER BY dt
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS cumulative_mtd
FROM daily
ORDER BY dt;

-- ============================================
-- 5. Trailing 28-day revenue (RANGE + INTERVAL)
-- ============================================
-- RANGE with INTERVAL is gap-safe without needing a spine
WITH daily AS (
    SELECT DATE(created_at) AS dt,
           SUM(amount)      AS revenue
    FROM orders
    GROUP BY DATE(created_at)
)
SELECT dt,
       revenue,
       SUM(revenue) OVER (
           ORDER BY dt::TIMESTAMP
           RANGE BETWEEN INTERVAL '27 days' PRECEDING AND CURRENT ROW
       ) AS trailing_28d
FROM daily
ORDER BY dt;

-- ============================================
-- 6. Rolling 3-period moving average (weekly grain)
-- ============================================
WITH weekly AS (
    SELECT DATE_TRUNC('week', created_at)::DATE AS week,
           SUM(amount)                          AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('week', created_at)
)
SELECT week,
       revenue,
       AVG(revenue) OVER (
           ORDER BY week
           ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
       ) AS rolling_3w_avg
FROM weekly
ORDER BY week;

-- ============================================
-- 7. Rolling active users (distinct users in last 7 days)
-- ============================================
-- ⚠ COUNT(DISTINCT) is not allowed inside a window frame.
-- Workaround: pre-agg to daily distinct users, then rolling sum.
-- This is approximate (a user active on multiple days counts > once),
-- but is the standard interview answer.
WITH daily_active AS (
    SELECT DATE(created_at) AS dt,
           COUNT(DISTINCT user_id) AS dau
    FROM sessions
    GROUP BY DATE(created_at)
)
SELECT dt,
       dau,
       SUM(dau) OVER (
           ORDER BY dt
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       ) AS rolling_7d_users  -- WAU approximation
FROM daily_active
ORDER BY dt;

-- ============================================
-- 8. LeetCode 1321 - Restaurant Growth
-- ============================================
-- Customer table: (customer_id, name, visited_on, amount)
-- Rolling 7-day window sum and average, starting from day 7

WITH daily AS (
    SELECT visited_on,
           SUM(amount) AS day_total
    FROM Customer
    GROUP BY visited_on
)
SELECT visited_on,
       SUM(day_total) OVER (
           ORDER BY visited_on
           ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
       )                                            AS amount,
       ROUND(
           AVG(day_total) OVER (
               ORDER BY visited_on
               ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
           ),
           2
       )                                            AS average_amount
FROM daily
ORDER BY visited_on
OFFSET 6;  -- first full 7-day window starts at row 7

-- ============================================
-- 9. Period-over-period with LAG (rolling context)
-- ============================================
WITH weekly AS (
    SELECT DATE_TRUNC('week', created_at)::DATE AS week,
           SUM(amount)                          AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('week', created_at)
)
SELECT week,
       revenue,
       LAG(revenue, 1) OVER (ORDER BY week)  AS prev_week,
       LAG(revenue, 4) OVER (ORDER BY week)  AS same_week_4w_ago,
       ROUND(
           100.0 * (revenue - LAG(revenue, 1) OVER (ORDER BY week))
               / NULLIF(LAG(revenue, 1) OVER (ORDER BY week), 0),
           2
       )                                     AS wow_growth_pct
FROM weekly
ORDER BY week;

-- ============================================
-- 10. Min/max in a rolling window (e.g. 30-day high/low)
-- ============================================
WITH daily AS (
    SELECT DATE(created_at) AS dt,
           AVG(price)       AS avg_price
    FROM stock_prices
    GROUP BY DATE(created_at)
)
SELECT dt,
       avg_price,
       MAX(avg_price) OVER (
           ORDER BY dt
           ROWS BETWEEN 29 PRECEDING AND CURRENT ROW
       ) AS rolling_30d_high,
       MIN(avg_price) OVER (
           ORDER BY dt
           ROWS BETWEEN 29 PRECEDING AND CURRENT ROW
       ) AS rolling_30d_low
FROM daily
ORDER BY dt;