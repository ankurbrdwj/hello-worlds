Ranking
You'll see: "top 3", "2nd highest", "latest order", "nth transaction"

What to do when you see it: use ROW_NUMBER() for exactly N rows per group; use RANK()/DENSE_RANK() if ties should be kept. Always add a deterministic tie-breaker.

SQL

SELECT *
FROM (
    SELECT
        t.*,
        ROW_NUMBER() OVER (
            PARTITION BY group_key
            ORDER BY sort_key DESC, tie_breaker DESC
        ) AS rn
    FROM t
) AS x
WHERE rn <= N;

Tip
"Top 3" with ties ← do they want exactly 3 rows or all ties at the cutoff?

-- ============================================
-- The three functions — when to use which
-- ============================================
-- Salaries: 100, 90, 90, 80
--
-- ROW_NUMBER:  1, 2, 3, 4  ← unique, arbitrary tie-break, use for "exactly N rows"
-- RANK:        1, 2, 2, 4  ← tie shares rank, next rank skips (gap at 3)
-- DENSE_RANK:  1, 2, 2, 3  ← tie shares rank, no gap — use for "top N salary levels"

-- ============================================
-- 1. Latest order per user (ROW_NUMBER)
-- ============================================
-- Exactly 1 row per user — ties broken by id DESC
SELECT user_id, order_id, amount, created_at
FROM (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM orders
) x
WHERE rn = 1;

-- ============================================
-- 2. Top 3 orders per user (ROW_NUMBER)
-- ============================================
-- Exactly 3 rows — if two orders tie on amount, lower id wins
SELECT user_id, order_id, amount
FROM (
    SELECT *,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY amount DESC, id ASC
           ) AS rn
    FROM orders
) x
WHERE rn <= 3;

-- ============================================
-- 3. Top 3 salary levels per department (DENSE_RANK)
-- ============================================
-- LeetCode 185 — ties at the cutoff are kept, no gaps
SELECT department, employee, salary
FROM (
    SELECT d.name AS department,
           e.name AS employee,
           e.salary,
           DENSE_RANK() OVER (
               PARTITION BY e.departmentId
               ORDER BY e.salary DESC
           ) AS rnk
    FROM Employee e
             JOIN Department d ON d.id = e.departmentId
) x
WHERE rnk <= 3;

-- ============================================
-- 4. 2nd highest salary overall (DENSE_RANK)
-- ============================================
-- DENSE_RANK handles the "no 2nd distinct salary" case cleanly — returns nothing
SELECT salary AS SecondHighestSalary
FROM (
    SELECT salary,
           DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM Employee
) x
WHERE rnk = 2
LIMIT 1;

-- Old way (correlated subquery, no window functions):
SELECT MAX(salary) AS SecondHighestSalary
FROM Employee
WHERE salary < (SELECT MAX(salary) FROM Employee);

-- ============================================
-- 5. Nth highest salary (generalised)
-- ============================================
-- Replace 3 with any N
SELECT salary
FROM (
    SELECT salary,
           DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM Employee
) x
WHERE rnk = 3   -- ← N here
LIMIT 1;

-- ============================================
-- 6. First purchase per user per product category
-- ============================================
SELECT user_id, category, order_id, amount, created_at
FROM (
    SELECT o.*,
           p.category,
           ROW_NUMBER() OVER (
               PARTITION BY o.user_id, p.category
               ORDER BY o.created_at ASC, o.id ASC
           ) AS rn
    FROM orders o
             JOIN products p ON p.id = o.product_id
) x
WHERE rn = 1;

-- ============================================
-- 7. Rank with TIES shown (RANK vs DENSE_RANK)
-- ============================================
-- Leaderboard: show position but flag tied players
SELECT user_id,
       score,
       RANK()       OVER (ORDER BY score DESC) AS rank_with_gap,
       DENSE_RANK() OVER (ORDER BY score DESC) AS rank_no_gap
FROM leaderboard;
-- rank_with_gap: 1,2,2,4 — position 3 skipped (gap shows "missing" spots)
-- rank_no_gap:   1,2,2,3 — continuous, easier to read for "top N levels"

-- ============================================
-- 8. LeetCode 177 - Nth Highest Salary (function)
-- ============================================
CREATE OR REPLACE FUNCTION NthHighestSalary(N INT) RETURNS TABLE (salary INT) AS
$$
BEGIN
    RETURN QUERY
        SELECT e.salary
        FROM (
            SELECT e2.salary,
                   DENSE_RANK() OVER (ORDER BY e2.salary DESC) AS rnk
            FROM Employee e2
        ) e
        WHERE e.rnk = N
        LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 9. LeetCode 1907 - Count Salary Categories
-- ============================================
-- Accounts(account_id, income)
-- Buckets: Low (<20k), Average (20k–50k), High (>50k)
-- Must return all 3 rows even if count = 0

SELECT 'Low Salary'     AS category,
       COUNT(*)         AS accounts_count
FROM Accounts WHERE income < 20000
UNION ALL
SELECT 'Average Salary',
       COUNT(*)
FROM Accounts WHERE income BETWEEN 20000 AND 50000
UNION ALL
SELECT 'High Salary',
       COUNT(*)
FROM Accounts WHERE income > 50000;

-- ============================================
-- 10. Remove duplicates: keep one row per group
-- ============================================
-- Common task: deduplicate a table, keep latest row per (user_id, email)
-- ❌ WRONG — DELETE without ranking often deletes too much or too little
-- ✅ CORRECT — rank first, delete rn > 1

DELETE FROM users
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY email
                   ORDER BY created_at DESC, id DESC
               ) AS rn
        FROM users
    ) x
    WHERE rn > 1
);