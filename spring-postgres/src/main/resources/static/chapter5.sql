-- ============================================
-- Chapter 5: LeetCode SQL - Hard Problems
-- ============================================

-- ============================================
-- Problem 185: Department Top Three Salaries
-- ============================================
-- URL: https://leetcode.com/problems/department-top-three-salaries/
-- Difficulty: Hard
--
-- Schema:
--   Employee(id, name, salary, departmentId)
--   Department(id, name)
--
-- Task: Find employees who have at most 3 unique salaries
--       that are >= their own salary within their department.
--       (i.e. they are in the top 3 salary earners per department)
-- ============================================

-- Setup
CREATE TABLE IF NOT EXISTS Department
(
    id   INT PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS Employee
(
    id           INT PRIMARY KEY,
    name         VARCHAR(50),
    salary       INT,
    departmentId INT REFERENCES Department (id)
);

INSERT INTO Department (id, name)
VALUES (1, 'IT'),
       (2, 'Sales')
ON CONFLICT DO NOTHING;

INSERT INTO Employee (id, name, salary, departmentId)
VALUES (1, 'Joe', 85000, 1),
       (2, 'Henry', 80000, 2),
       (3, 'Sam', 60000, 2),
       (4, 'Max', 90000, 1),
       (5, 'Janet', 69000, 1),
       (6, 'Randy', 85000, 1),
       (7, 'Will', 70000, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- Solution: DENSE_RANK() window function
-- ============================================
-- DENSE_RANK assigns consecutive ranks with no gaps on ties.
-- e.g. salaries 90k, 85k, 85k, 70k get ranks 1, 2, 2, 3
-- so 70k is still rank 3 and qualifies as "top 3"
-- (ROW_NUMBER or RANK would wrongly exclude it)
-- ============================================

WITH ranked AS (
    SELECT e.name         AS Employee,
           e.salary,
           d.name         AS Department,
           DENSE_RANK() OVER (
               PARTITION BY e.departmentId
               ORDER BY e.salary DESC
               )          AS salary_rank
    FROM Employee e
             JOIN Department d ON d.id = e.departmentId
)
SELECT Department,
       Employee,
       salary
FROM ranked
WHERE salary_rank <= 3
ORDER BY Department, salary DESC;

-- ============================================
-- Old way (no window functions): correlated subquery
-- ============================================
-- Logic: an employee is in the top 3 if fewer than 3
-- *distinct* salaries in their department are strictly
-- greater than their own salary.
-- ============================================

SELECT d.name  AS Department,
       e.name  AS Employee,
       e.salary
FROM Employee e
         JOIN Department d ON d.id = e.departmentId
WHERE (
          SELECT COUNT(DISTINCT e2.salary)
          FROM Employee e2
          WHERE e2.departmentId = e.departmentId  -- same dept
            AND e2.salary > e.salary              -- strictly higher
      ) < 3
ORDER BY d.name, e.salary DESC;

-- How to read it:
--   For Max  (90k, IT):   0 salaries above him  → 0 < 3 ✓
--   For Joe  (85k, IT):   1 salary above (90k)  → 1 < 3 ✓
--   For Will (70k, IT):   2 salaries above       → 2 < 3 ✓
--   For Janet(69k, IT):   3 salaries above       → 3 < 3 ✗ excluded

-- ============================================
-- Expected output:
-- Department | Employee | Salary
-- -----------+----------+-------
-- IT         | Max      | 90000
-- IT         | Randy    | 85000
-- IT         | Joe      | 85000
-- IT         | Will     | 70000   ← rank 3 (ties at rank 2 don't consume rank 3)
-- Sales      | Henry    | 80000
-- Sales      | Sam      | 60000