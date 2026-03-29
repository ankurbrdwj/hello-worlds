Conditional Aggregation
You'll see: "count X and Y in one table", "funnel by step", "segment by device"

What to do when you see it: keep the grain stable, and use SUM(CASE WHEN ...) (or COUNT with NULLs) to compute multiple metrics in one pass.

SQL

SELECT
    group_key,
    SUM(CASE WHEN condition_1 THEN 1 ELSE 0 END) AS metric_1,
    SUM(CASE WHEN condition_2 THEN 1 ELSE 0 END) AS metric_2
FROM t
GROUP BY group_key;

Tip
Are we counting rows, or unique users? ← May need COUNT(DISTINCT user_id) or pre-agg.

-- ============================================
-- 1. Orders vs cancellations per user (classic)
-- ============================================
-- One pass over orders table, two metrics out.
SELECT user_id,
       COUNT(*)                                              AS total_orders,
       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled
FROM orders
GROUP BY user_id;

-- ============================================
-- 2. Funnel by step (signup → verify → purchase)
-- ============================================
-- events table: (user_id, event_name, created_at)
SELECT
    SUM(CASE WHEN event_name = 'signup'   THEN 1 ELSE 0 END) AS signups,
    SUM(CASE WHEN event_name = 'verify'   THEN 1 ELSE 0 END) AS verifies,
    SUM(CASE WHEN event_name = 'purchase' THEN 1 ELSE 0 END) AS purchases
FROM events;

-- Funnel per day
SELECT DATE(created_at)                                          AS day,
       SUM(CASE WHEN event_name = 'signup' THEN 1 ELSE 0 END)   AS signups,
       SUM(CASE WHEN event_name = 'verify' THEN 1 ELSE 0 END)   AS verifies,
       SUM(CASE WHEN event_name = 'purchase' THEN 1 ELSE 0 END) AS purchases
FROM events
GROUP BY DATE(created_at)
ORDER BY day;

-- ============================================
-- 3. Segment by device type
-- ============================================
-- sessions table: (user_id, device, revenue)
SELECT user_id,
       SUM(CASE WHEN device = 'mobile'  THEN revenue ELSE 0 END) AS mobile_revenue,
       SUM(CASE WHEN device = 'desktop' THEN revenue ELSE 0 END) AS desktop_revenue,
       SUM(CASE WHEN device = 'tablet'  THEN revenue ELSE 0 END) AS tablet_revenue,
       SUM(revenue)                                               AS total_revenue
FROM sessions
GROUP BY user_id;

-- ============================================
-- 4. COUNT(NULLS) shorthand (cleaner alternative)
-- ============================================
-- CASE...ELSE NULL means COUNT only increments on truthy rows.
-- COUNT ignores NULLs — so this is equivalent to SUM(CASE WHEN ... THEN 1 ELSE 0)
SELECT user_id,
       COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed,
       COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled
FROM orders
GROUP BY user_id;

-- ============================================
-- 5. Unique users per segment (COUNT DISTINCT trap)
-- ============================================
-- ❌ WRONG — double counts users who appear in both segments
-- SELECT
--     COUNT(DISTINCT CASE WHEN plan = 'free' THEN user_id END) AS free_users,
--     COUNT(DISTINCT CASE WHEN plan = 'paid' THEN user_id END) AS paid_users
-- FROM subscriptions;

-- ✅ CORRECT — pre-agg to deduplicate first
WITH latest_plan AS (
    SELECT DISTINCT ON (user_id) user_id,
                                 plan
    FROM subscriptions
    ORDER BY user_id, started_at DESC
)
SELECT SUM(CASE WHEN plan = 'free' THEN 1 ELSE 0 END) AS free_users,
       SUM(CASE WHEN plan = 'paid' THEN 1 ELSE 0 END) AS paid_users
FROM latest_plan;

-- ============================================
-- 6. Revenue split: domestic vs international
-- ============================================
SELECT DATE_TRUNC('month', o.created_at)                              AS month,
       SUM(CASE WHEN c.country = 'US' THEN o.amount ELSE 0 END)      AS domestic,
       SUM(CASE WHEN c.country != 'US' THEN o.amount ELSE 0 END)     AS international
FROM orders o
         JOIN customers c ON c.id = o.customer_id
GROUP BY DATE_TRUNC('month', o.created_at)
ORDER BY month;

-- ============================================
-- 7. LeetCode 1393 - Capital Gain/Loss
-- ============================================
-- Stocks table: (stock_name, operation [Buy/Sell], price)
-- Task: net capital = SUM of sell prices - SUM of buy prices
SELECT stock_name,
       SUM(CASE WHEN operation = 'Sell' THEN price ELSE -price END) AS capital_gain_loss
FROM Stocks
GROUP BY stock_name;

-- ============================================
-- 8. LeetCode 1341 - Movie Rating (conditional agg + UNION)
-- ============================================
-- Find: (a) user who rated the most movies, (b) movie with highest avg rating in Feb 2020
-- Uses conditional agg for the filter, UNION to combine two different grains into one result

(SELECT u.name AS results
 FROM MovieRating mr
          JOIN Users u ON u.user_id = mr.user_id
 GROUP BY u.name
 ORDER BY COUNT(*) DESC, u.name
 LIMIT 1)

UNION ALL

(SELECT m.title AS results
 FROM MovieRating mr
          JOIN Movies m ON m.movie_id = mr.movie_id
 WHERE mr.created_at BETWEEN '2020-02-01' AND '2020-02-29'
 GROUP BY m.title
 ORDER BY AVG(mr.rating) DESC, m.title
 LIMIT 1);

-- ============================================
-- 9. Pivot: monthly order counts per status
-- ============================================
-- Transform rows (status) into columns — classic pivot via conditional agg
SELECT DATE_TRUNC('month', created_at)                                  AS month,
       SUM(CASE WHEN status = 'PENDING'   THEN 1 ELSE 0 END)           AS pending,
       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END)           AS completed,
       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END)           AS cancelled
FROM orders
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month;

-- ============================================
-- 10. Ratio / percentage with conditional agg
-- ============================================
-- Completion rate per campaign
SELECT campaign_id,
       COUNT(*)                                                        AS total,
       SUM(CASE WHEN completed THEN 1 ELSE 0 END)                     AS completed_count,
       ROUND(
               100.0 * SUM(CASE WHEN completed THEN 1 ELSE 0 END)
                   / NULLIF(COUNT(*), 0),
               2
       )                                                               AS completion_pct
FROM campaign_responses
GROUP BY campaign_id;

-- Note: NULLIF(COUNT(*), 0) guards against divide-by-zero when a campaign has no rows.