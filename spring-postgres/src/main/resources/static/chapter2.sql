-- ============================================
-- 1. Users who have at least one order
-- ============================================
SELECT u.*
FROM users u
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
);

-- ============================================
-- 2. Users who NEVER placed any order
-- ============================================
SELECT u.*
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
);

-- ============================================
-- 3. Customers who have at least one HIGH VALUE order (> 1000)
-- ============================================
SELECT c.*
FROM customers c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
      AND o.amount > 1000
);

-- ============================================
-- 4. Customers who never bought iPhone 12
-- ============================================
SELECT c.*
FROM customers c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
      AND o.product = 'iPhone 12'
);

-- ============================================
-- 5. Employees who have at least one report (manager relationship)
-- ============================================
SELECT e.*
FROM employees e
WHERE EXISTS (
    SELECT 1
    FROM employees sub
    WHERE sub.manager_id = e.id
);

-- ============================================
-- 6. Employees who are NOT managers (no reports)
-- ============================================
SELECT e.*
FROM employees e
WHERE NOT EXISTS (
    SELECT 1
    FROM employees sub
    WHERE sub.manager_id = e.id
);

-- ============================================
-- 7. Products that have NEVER been ordered
-- ============================================
SELECT p.*
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.product_id = p.id
);

-- ============================================
-- 8. Clients whose CURRENT phone is iPhone 12
-- (Combining EXISTS + latest record logic)
-- ============================================
SELECT p1.client_id
FROM phone_log p1
WHERE p1.model = 'iPhone 12'
  AND NOT EXISTS (
    SELECT 1
    FROM phone_log p2
    WHERE p2.client_id = p1.client_id
      AND p2.date > p1.date
);

-- Explanation:
-- "Pick rows where no newer record exists" → ensures current record

-- ============================================
-- 9. Orders that have at least one payment
-- ============================================
SELECT o.*
FROM orders o
WHERE EXISTS (
    SELECT 1
    FROM payments p
    WHERE p.order_id = o.id
);

-- ============================================
-- 10. Orders that are NOT paid
-- ============================================
SELECT o.*
FROM orders o
WHERE NOT EXISTS (
    SELECT 1
    FROM payments p
    WHERE p.order_id = o.id
);

-- ============================================
-- 11. Users who logged in from multiple devices
-- ============================================
SELECT u.*
FROM users u
WHERE EXISTS (
    SELECT 1
    FROM logins l1
             JOIN logins l2
                  ON l1.user_id = l2.user_id
                      AND l1.device <> l2.device
    WHERE l1.user_id = u.id
);

-- ============================================
-- 12. Duplicate-safe filtering (important interview point)
-- ============================================fffffffx
SELECT u.*
FROM users u
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
);

-- Note:
-- EXISTS stops at first match → no duplicate explosion
-- JOIN would duplicate rows if multiple orders exist