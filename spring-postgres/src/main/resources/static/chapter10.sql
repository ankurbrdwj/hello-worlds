Previous vs current
You'll see: "week-over-week change", "difference from previous", "growth rate"

What to do when you see it: decide if "previous" means previous row or previous period (calendar-aligned), then use LAG().

SQL

SELECT
    key,
    dt,
    metric,
    metric - LAG(metric) OVER (
        PARTITION BY key
        ORDER BY dt
    ) AS delta
FROM t;

Tip
For period-over-period (e.g., "previous month"), ensure your dt is already bucketed to month.

-- ============================================
-- LAG / LEAD reference
-- ============================================
-- LAG(col, N)  → value N rows BEHIND current row  (default N=1)
-- LEAD(col, N) → value N rows AHEAD  of current row (default N=1)
-- Both return NULL when the offset falls outside the partition.
-- Optional 3rd arg: default value when NULL  e.g. LAG(col, 1, 0)

-- ============================================
-- 1. Week-over-week revenue change (absolute + %)
-- ============================================
WITH weekly AS (
    SELECT DATE_TRUNC('week', created_at)::DATE AS week,
           SUM(amount)                          AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('week', created_at)
)
SELECT week,
       revenue,
       LAG(revenue) OVER (ORDER BY week)                        AS prev_week_revenue,
       revenue - LAG(revenue) OVER (ORDER BY week)              AS wow_delta,
       ROUND(
           100.0 * (revenue - LAG(revenue) OVER (ORDER BY week))
               / NULLIF(LAG(revenue) OVER (ORDER BY week), 0),
           2
       )                                                        AS wow_pct
FROM weekly
ORDER BY week;

-- ============================================
-- 2. Month-over-month per product category
-- ============================================
-- PARTITION BY category — each category gets its own "previous month"
WITH monthly AS (
    SELECT DATE_TRUNC('month', o.created_at)::DATE AS month,
           p.category,
           SUM(o.amount)                           AS revenue
    FROM orders o
             JOIN products p ON p.id = o.product_id
    GROUP BY DATE_TRUNC('month', o.created_at), p.category
)
SELECT month,
       category,
       revenue,
       LAG(revenue) OVER (PARTITION BY category ORDER BY month) AS prev_month,
       revenue - LAG(revenue) OVER (PARTITION BY category ORDER BY month) AS mom_delta
FROM monthly
ORDER BY category, month;

-- ============================================
-- 3. Same period last year (LAG offset = 12 months)
-- ============================================
WITH monthly AS (
    SELECT DATE_TRUNC('month', created_at)::DATE AS month,
           SUM(amount)                           AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('month', created_at)
)
SELECT month,
       revenue,
       LAG(revenue, 12) OVER (ORDER BY month)   AS same_month_last_year,
       ROUND(
           100.0 * (revenue - LAG(revenue, 12) OVER (ORDER BY month))
               / NULLIF(LAG(revenue, 12) OVER (ORDER BY month), 0),
           2
       )                                        AS yoy_pct
FROM monthly
ORDER BY month;

-- ============================================
-- 4. Delta from previous row (any grain)
-- ============================================
-- Generic pattern — works for daily, weekly, or per-user sequences
SELECT user_id,
       event_date,
       score,
       score - LAG(score) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                        AS score_change
FROM user_scores
ORDER BY user_id, event_date;

-- ============================================
-- 5. First vs latest value per user
-- ============================================
-- FIRST_VALUE / LAST_VALUE are cleaner than LAG(N) for this
SELECT user_id,
       event_date,
       score,
       FIRST_VALUE(score) OVER (
           PARTITION BY user_id
           ORDER BY event_date
           ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
       )                                        AS first_score,
       LAST_VALUE(score) OVER (
           PARTITION BY user_id
           ORDER BY event_date
           ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
       )                                        AS latest_score
FROM user_scores;

-- ⚠ LAST_VALUE needs explicit ROWS UNBOUNDED FOLLOWING.
--   Default frame is RANGE UNBOUNDED PRECEDING TO CURRENT ROW,
--   so without it LAST_VALUE just returns the current row.

-- ============================================
-- 6. LEAD — look ahead (next period preview)
-- ============================================
-- "What is the next event for this user?"
SELECT user_id,
       event_date,
       event_type,
       LEAD(event_type) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                        AS next_event,
       LEAD(event_date) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                        AS next_event_date
FROM user_events
ORDER BY user_id, event_date;

-- ============================================
-- 7. Days between consecutive events (session gap)
-- ============================================
SELECT user_id,
       event_date,
       LAG(event_date) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                              AS prev_event_date,
       event_date - LAG(event_date) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                              AS days_since_last
FROM user_events
ORDER BY user_id, event_date;

-- ============================================
-- 8. LeetCode 180 - Consecutive Numbers
-- ============================================
-- Find numbers that appear at least 3 times consecutively
-- Logs table: (id, num)
SELECT DISTINCT l1.num AS ConsecutiveNums
FROM Logs l1
         JOIN Logs l2 ON l2.id = l1.id + 1 AND l2.num = l1.num
         JOIN Logs l3 ON l3.id = l1.id + 2 AND l3.num = l1.num;

-- Window function alternative (cleaner):
SELECT DISTINCT num AS ConsecutiveNums
FROM (
    SELECT num,
           LAG(num, 1) OVER (ORDER BY id)  AS prev1,
           LAG(num, 2) OVER (ORDER BY id)  AS prev2
    FROM Logs
) x
WHERE num = prev1 AND num = prev2;

-- ============================================
-- 9. LeetCode 196 / 197 - Rising Temperature
-- ============================================
-- Weather table: (id, recordDate, temperature)
-- Find days where temperature is higher than the previous day
SELECT w1.id
FROM Weather w1
         JOIN Weather w2
              ON w2.recordDate = w1.recordDate - INTERVAL '1 day'
WHERE w1.temperature > w2.temperature;

-- LAG alternative:
SELECT id
FROM (
    SELECT id,
           temperature,
           LAG(temperature) OVER (ORDER BY recordDate) AS prev_temp,
           recordDate - LAG(recordDate) OVER (ORDER BY recordDate) AS day_gap
    FROM Weather
) x
WHERE temperature > prev_temp
  AND day_gap = 1;  -- guard: skip if previous calendar day is missing

-- ============================================
-- 10. Detect state changes (e.g. status transitions)
-- ============================================
-- "Show rows where status changed from the previous row"
SELECT user_id,
       event_date,
       status,
       LAG(status) OVER (
           PARTITION BY user_id
           ORDER BY event_date
       )                                        AS prev_status
FROM user_status_log
WHERE status != LAG(status) OVER (
    PARTITION BY user_id
    ORDER BY event_date
);
-- ⚠ Window functions can't be used directly in WHERE.
-- Wrap in a subquery:
SELECT *
FROM (
    SELECT user_id,
           event_date,
           status,
           LAG(status) OVER (
               PARTITION BY user_id
               ORDER BY event_date
           ) AS prev_status
    FROM user_status_log
) x
WHERE status != prev_status
   OR prev_status IS NULL;  -- include first event (no previous)