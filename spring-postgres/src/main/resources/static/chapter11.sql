Self-joins
You'll see: "pairs of items", "before/after events", "employee-manager", "referrer relationships"

What to do when you see it: alias the same table twice, and write a rule to avoid duplicates or self-pairs (a.id < b.id is a common trick).

SQL

SELECT
    a.id AS left_id,
    b.id AS right_id
FROM t AS a
JOIN t AS b
    ON <pair_condition>
WHERE a.id < b.id;

Tip
Pair explosions are real ← filter the dataset first (time window, paid only, etc.).

-- ============================================
-- Duplicate / self-pair avoidance cheat-sheet
-- ============================================
-- a.id < b.id   → unordered pairs, no self-pairs, no duplicates  (most common)
-- a.id != b.id  → ordered pairs, no self-pairs, but (A,B) and (B,A) both appear
-- a.id < b.id + extra filter → directed pairs with dedup

-- ============================================
-- 1. Employee → manager (classic hierarchy)
-- ============================================
-- Employee(id, name, managerId)
SELECT e.name   AS employee,
       m.name   AS manager
FROM Employee e
         JOIN Employee m ON m.id = e.managerId;

-- ============================================
-- 2. LeetCode 181 - Employees Earning More Than Manager
-- ============================================
SELECT e.name AS Employee
FROM Employee e
         JOIN Employee m ON m.id = e.managerId
WHERE e.salary > m.salary;

-- ============================================
-- 3. LeetCode 197 - Find all pairs in same dept with salary gap > 10k
-- ============================================
-- Unordered pairs only: a.id < b.id
SELECT a.name   AS employee_a,
       b.name   AS employee_b,
       ABS(a.salary - b.salary) AS salary_gap
FROM Employee a
         JOIN Employee b
              ON a.departmentId = b.departmentId
                  AND a.id < b.id
WHERE ABS(a.salary - b.salary) > 10000
ORDER BY salary_gap DESC;

-- ============================================
-- 4. Users who referred each other (mutual referral)
-- ============================================
-- referrals(referrer_id, referred_id)
SELECT a.referrer_id AS user_a,
       a.referred_id AS user_b
FROM referrals a
         JOIN referrals b
              ON b.referrer_id = a.referred_id
                  AND b.referred_id = a.referrer_id
WHERE a.referrer_id < a.referred_id;  -- dedup: keep one direction

-- ============================================
-- 5. Before/after: sessions within 30 minutes of each other
-- ============================================
-- Find pairs of sessions from the same user that overlap within 30 min
SELECT a.user_id,
       a.session_id AS session_a,
       b.session_id AS session_b,
       a.started_at,
       b.started_at AS next_started_at
FROM sessions a
         JOIN sessions b
              ON b.user_id = a.user_id
                  AND b.started_at > a.started_at
                  AND b.started_at <= a.started_at + INTERVAL '30 minutes'
ORDER BY a.user_id, a.started_at;

-- ============================================
-- 6. LeetCode 180 - Consecutive Numbers (self-join version)
-- ============================================
-- Already covered in ch10 but shown here as a self-join pattern
SELECT DISTINCT l1.num AS ConsecutiveNums
FROM Logs l1
         JOIN Logs l2 ON l2.id = l1.id + 1 AND l2.num = l1.num
         JOIN Logs l3 ON l3.id = l1.id + 2 AND l3.num = l1.num;

-- ============================================
-- 7. Products bought together (market basket)
-- ============================================
-- order_items(order_id, product_id)
-- Find product pairs that appear in the same order
SELECT a.product_id AS product_a,
       b.product_id AS product_b,
       COUNT(DISTINCT a.order_id) AS co_occurrence_count
FROM order_items a
         JOIN order_items b
              ON b.order_id = a.order_id
                  AND a.product_id < b.product_id   -- unordered pairs, no self-pairs
GROUP BY a.product_id, b.product_id
ORDER BY co_occurrence_count DESC;

-- ⚠ Pair explosion risk: 1000 products → up to 500k pairs.
-- Pre-filter to a time window or top-N products first.

-- ============================================
-- 8. Users active on two consecutive days (self-join on date)
-- ============================================
WITH daily AS (
    SELECT DISTINCT user_id, DATE(created_at) AS dt
    FROM sessions
)
SELECT a.user_id,
       a.dt      AS day_1,
       b.dt      AS day_2
FROM daily a
         JOIN daily b
              ON b.user_id = a.user_id
                  AND b.dt = a.dt + INTERVAL '1 day'
ORDER BY a.user_id, a.dt;

-- ============================================
-- 9. LeetCode 601 - Human Traffic of Stadium
-- ============================================
-- Stadium(id, visit_date, people)
-- Find rows with >= 100 people for 3 or more consecutive ids
WITH filtered AS (
    SELECT *
    FROM Stadium
    WHERE people >= 100
)
SELECT DISTINCT s1.*
FROM filtered s1
         JOIN filtered s2 ON s2.id IN (s1.id + 1, s1.id - 1)
         JOIN filtered s3 ON s3.id IN (s1.id + 1, s1.id - 1, s1.id + 2, s1.id - 2)
WHERE s2.id != s3.id
ORDER BY s1.id;

-- Cleaner alternative using window functions:
WITH filtered AS (
    SELECT *,
           COUNT(*) OVER (
               ORDER BY id
               ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
           ) AS cnt_before,
           COUNT(*) OVER (
               ORDER BY id
               ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
           ) AS cnt_around,
           COUNT(*) OVER (
               ORDER BY id
               ROWS BETWEEN CURRENT ROW AND 2 FOLLOWING
           ) AS cnt_after
    FROM Stadium
    WHERE people >= 100
)
SELECT id, visit_date, people
FROM filtered
WHERE cnt_before = 3 OR cnt_around = 3 OR cnt_after = 3
ORDER BY id;

-- ============================================
-- 10. Org chart depth (recursive self-join)
-- ============================================
-- Walk an employee hierarchy up to the root
WITH RECURSIVE hierarchy AS (
    -- anchor: start from a specific employee
    SELECT id, name, managerId, 0 AS depth
    FROM Employee
    WHERE managerId IS NULL   -- top of the tree

    UNION ALL

    SELECT e.id, e.name, e.managerId, h.depth + 1
    FROM Employee e
             JOIN hierarchy h ON h.id = e.managerId
)
SELECT id, name, depth
FROM hierarchy
ORDER BY depth, id;