Rates, shares, and NULL-safe division
You'll see: "conversion rate", "CTR", "share of total", "percent of users who…"

What to do when you see it: define numerator/denominator explicitly, force float math, and guard divide-by-zero with NULLIF.

SQL

SELECT group_key,
       numerator,
       denominator,
       100.0 * numerator / NULLIF(denominator, 0) AS pct
FROM (...);

Tip
Rate based on "users" or "events"? ← Changes the denominator.
Should empty groups show NULL, 0, or be excluded?

-- ============================================
-- 1. Conversion rate per campaign (events-based)
-- ============================================
-- denominator = all events, numerator = converted events
SELECT campaign_id,
       COUNT(*)                                                            AS total_events,
       SUM(CASE WHEN converted THEN 1 ELSE 0 END)                        AS conversions,
       ROUND(
           100.0 * SUM(CASE WHEN converted THEN 1 ELSE 0 END)
               / NULLIF(COUNT(*), 0),
           2
       )                                                                  AS conversion_rate_pct
FROM events
GROUP BY campaign_id;

-- ============================================
-- 2. Conversion rate per campaign (users-based)
-- ============================================
-- ⚠ Different denominator → different number!
-- events-based: 4 conversions / 100 clicks = 4%
-- users-based:  3 unique converters / 60 unique visitors = 5%
WITH user_level AS (
    SELECT campaign_id,
           user_id,
           MAX(CASE WHEN converted THEN 1 ELSE 0 END) AS did_convert
    FROM events
    GROUP BY campaign_id, user_id
)
SELECT campaign_id,
       COUNT(*)                                              AS unique_users,
       SUM(did_convert)                                     AS converted_users,
       ROUND(100.0 * SUM(did_convert) / NULLIF(COUNT(*), 0), 2) AS user_conversion_pct
FROM user_level
GROUP BY campaign_id;

-- ============================================
-- 3. Click-through rate (CTR)
-- ============================================
-- impressions table: (ad_id, user_id, clicked)
SELECT ad_id,
       COUNT(*)                                                AS impressions,
       SUM(CASE WHEN clicked THEN 1 ELSE 0 END)              AS clicks,
       ROUND(
           100.0 * SUM(CASE WHEN clicked THEN 1 ELSE 0 END)
               / NULLIF(COUNT(*), 0),
           2
       )                                                      AS ctr_pct
FROM impressions
GROUP BY ad_id
ORDER BY ctr_pct DESC;

-- ============================================
-- 4. Share of total (each group's % of grand total)
-- ============================================
-- revenue per product as % of all revenue
SELECT product_id,
       SUM(amount)                                            AS product_revenue,
       SUM(SUM(amount)) OVER ()                              AS grand_total,
       ROUND(
           100.0 * SUM(amount)
               / NULLIF(SUM(SUM(amount)) OVER (), 0),
           2
       )                                                      AS revenue_share_pct
FROM orders
GROUP BY product_id
ORDER BY revenue_share_pct DESC;

-- Note: SUM(SUM(amount)) OVER () is a window over the grouped result —
--       inner SUM aggregates to product level, outer SUM totals across all products.

-- ============================================
-- 5. Share within group (% within each country)
-- ============================================
SELECT country,
       product_id,
       SUM(amount)                                            AS revenue,
       SUM(SUM(amount)) OVER (PARTITION BY country)          AS country_total,
       ROUND(
           100.0 * SUM(amount)
               / NULLIF(SUM(SUM(amount)) OVER (PARTITION BY country), 0),
           2
       )                                                      AS share_within_country_pct
FROM orders o
         JOIN customers c ON c.id = o.customer_id
GROUP BY country, product_id;

-- ============================================
-- 6. Retention rate week-over-week
-- ============================================
-- sessions table: (user_id, week)
-- "of users active in week N, how many returned in week N+1?"
WITH week_users AS (
    SELECT DISTINCT user_id, week
    FROM sessions
),
     consecutive AS (
         SELECT w1.week                  AS week,
                COUNT(DISTINCT w1.user_id) AS active_users,
                COUNT(DISTINCT w2.user_id) AS retained_users
         FROM week_users w1
                  LEFT JOIN week_users w2
                            ON w2.user_id = w1.user_id
                                AND w2.week = w1.week + 1
         GROUP BY w1.week
     )
SELECT week,
       active_users,
       retained_users,
       ROUND(100.0 * retained_users / NULLIF(active_users, 0), 2) AS retention_pct
FROM consecutive
ORDER BY week;

-- ============================================
-- 7. NULL-safe division — three output choices
-- ============================================
-- When denominator is 0 or NULL, pick one:

-- (a) Return NULL  ← use NULLIF, leave as-is
SELECT 100.0 * numerator / NULLIF(denominator, 0) AS pct  -- NULL when 0
FROM t;

-- (b) Return 0     ← wrap with COALESCE
SELECT COALESCE(100.0 * numerator / NULLIF(denominator, 0), 0) AS pct
FROM t;

-- (c) Exclude row  ← filter in WHERE
SELECT *
FROM t
WHERE denominator > 0;

-- ============================================
-- 8. LeetCode 1934 - Confirmation Rate
-- ============================================
-- Signups(user_id) + Confirmations(user_id, action ['confirmed'/'timeout'])
-- Rate = confirmed / total requests; users with no requests → 0.00
SELECT s.user_id,
       ROUND(
           COALESCE(
               AVG(CASE WHEN c.action = 'confirmed' THEN 1.0 ELSE 0.0 END),
               0
           ),
           2
       ) AS confirmation_rate
FROM Signups s
         LEFT JOIN Confirmations c ON c.user_id = s.user_id
GROUP BY s.user_id;

-- Note: AVG over 1.0/0.0 is equivalent to COUNT(confirmed)/COUNT(total).
--       LEFT JOIN keeps users with zero confirmations; COALESCE handles their NULL avg.

-- ============================================
-- 9. LeetCode 1211 - Queries Quality and Percentage
-- ============================================
-- Queries(query_name, result, position, rating)
-- quality     = AVG(rating / position)
-- poor_query_percentage = % of queries with rating < 3
SELECT query_name,
       ROUND(AVG(1.0 * rating / position), 2)                           AS quality,
       ROUND(
           100.0 * SUM(CASE WHEN rating < 3 THEN 1 ELSE 0 END)
               / NULLIF(COUNT(*), 0),
           2
       )                                                                 AS poor_query_percentage
FROM Queries
WHERE query_name IS NOT NULL
GROUP BY query_name;

-- ============================================
-- 10. Percent change period-over-period
-- ============================================
-- "revenue grew X% vs last month"
WITH monthly AS (
    SELECT DATE_TRUNC('month', created_at) AS month,
           SUM(amount)                     AS revenue
    FROM orders
    GROUP BY DATE_TRUNC('month', created_at)
)
SELECT month,
       revenue,
       LAG(revenue) OVER (ORDER BY month)                                AS prev_revenue,
       ROUND(
           100.0 * (revenue - LAG(revenue) OVER (ORDER BY month))
               / NULLIF(LAG(revenue) OVER (ORDER BY month), 0),
           2
       )                                                                 AS growth_pct
FROM monthly
ORDER BY month;