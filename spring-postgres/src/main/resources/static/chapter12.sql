Brokerage & trading SQL — positions, cost basis and P&L from a fills log
You'll see: "current position", "cost basis", "realized vs unrealized P&L", "portfolio value", "fill rate", "VWAP", "wash trade"

What to do when you see it: treat `fills` as an immutable log and rebuild state from it — use a signed quantity (buy = +qty, sell = -qty) so "position" is a plain SUM, weight prices by qty for any average, and join to the latest `market_data` close for anything "unrealized".

SQL

SELECT account_id,
       symbol,
       SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END)              AS net_qty,
       SUM(CASE WHEN side = 'buy' THEN qty * price ELSE 0 END)
           / NULLIF(SUM(CASE WHEN side = 'buy' THEN qty ELSE 0 END), 0) AS avg_cost
FROM fills
GROUP BY account_id, symbol
HAVING SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END) <> 0;   -- drop closed positions

Tip
Average-cost or FIFO? ← average-cost is one GROUP BY (buy_cost / buy_qty). FIFO needs
    the sells matched against buys in order — a running SUM window or a LATERAL join.
Which "price" is the mark? ← latest close for end-of-day P&L, last fill for a live blotter.
Sign convention first: buy = +qty / cash out, sell = -qty / cash in. Everything else follows.

-- ============================================
-- 1. Current position per account & symbol
-- ============================================
-- Signed SUM of qty. HAVING hides positions that netted flat (fully closed).
-- cash_delta: negative = net cash paid out to build the position.
SELECT account_id,
       symbol,
       SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END)              AS net_qty,
       SUM(CASE WHEN side = 'buy' THEN -(qty * price) ELSE qty * price END) AS cash_delta
FROM fills
GROUP BY account_id, symbol
HAVING SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END) <> 0
ORDER BY account_id, symbol;

-- ============================================
-- 2. Average cost basis of each open position
-- ============================================
-- Average-cost method: avg_cost = total buy cost / total buy qty (over ALL buys).
-- remaining_cost_basis = avg_cost * shares still held.
WITH pos AS (
    SELECT account_id,
           symbol,
           SUM(CASE WHEN side = 'buy' THEN qty ELSE 0 END)             AS buy_qty,
           SUM(CASE WHEN side = 'buy' THEN qty * price ELSE 0 END)     AS buy_cost,
           SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END)          AS net_qty
    FROM fills
    GROUP BY account_id, symbol
)
SELECT account_id,
       symbol,
       net_qty,
       ROUND(buy_cost / NULLIF(buy_qty, 0), 4)                         AS avg_cost,
       ROUND(net_qty * buy_cost / NULLIF(buy_qty, 0), 2)              AS remaining_cost_basis
FROM pos
WHERE net_qty <> 0
ORDER BY account_id, symbol;

-- ============================================
-- 3. VWAP per symbol per day (from fills)
-- ============================================
-- Volume-weighted average price = SUM(qty * price) / SUM(qty). Never AVG(price).
SELECT symbol,
       filled_at::date                                                AS trade_date,
       SUM(qty)                                                       AS shares_traded,
       ROUND(SUM(qty * price) / NULLIF(SUM(qty), 0), 4)              AS vwap
FROM fills
GROUP BY symbol, filled_at::date
ORDER BY symbol, trade_date;

-- ============================================
-- 4. Realized P&L (average-cost method)
-- ============================================
-- Only the shares actually SOLD produce realized P&L:
--   realized = sell_proceeds - sold_qty * avg_buy_cost
WITH agg AS (
    SELECT account_id,
           symbol,
           SUM(CASE WHEN side = 'buy'  THEN qty       ELSE 0 END)      AS buy_qty,
           SUM(CASE WHEN side = 'buy'  THEN qty * price ELSE 0 END)    AS buy_cost,
           SUM(CASE WHEN side = 'sell' THEN qty       ELSE 0 END)      AS sell_qty,
           SUM(CASE WHEN side = 'sell' THEN qty * price ELSE 0 END)    AS sell_proceeds
    FROM fills
    GROUP BY account_id, symbol
)
SELECT account_id,
       symbol,
       sell_qty,
       ROUND(sell_proceeds - sell_qty * (buy_cost / NULLIF(buy_qty, 0)), 2) AS realized_pnl
FROM agg
WHERE sell_qty > 0
ORDER BY account_id, symbol;

-- ============================================
-- 5. Unrealized P&L (mark to latest close)
-- ============================================
-- unrealized = net_qty * (latest_close - avg_cost)
-- DISTINCT ON grabs the newest bar per symbol.
WITH latest_px AS (
    SELECT DISTINCT ON (symbol) symbol, trade_date, close
    FROM market_data
    ORDER BY symbol, trade_date DESC
),
     pos AS (
         SELECT account_id,
                symbol,
                SUM(CASE WHEN side = 'buy' THEN qty ELSE -qty END)     AS net_qty,
                SUM(CASE WHEN side = 'buy' THEN qty ELSE 0 END)        AS buy_qty,
                SUM(CASE WHEN side = 'buy' THEN qty * price ELSE 0 END) AS buy_cost
         FROM fills
         GROUP BY account_id, symbol
     )
SELECT p.account_id,
       p.symbol,
       p.net_qty,
       l.close                                                        AS mark_price,
       ROUND(p.buy_cost / NULLIF(p.buy_qty, 0), 4)                    AS avg_cost,
       ROUND(p.net_qty * (l.close - p.buy_cost / NULLIF(p.buy_qty, 0)), 2) AS unrealized_pnl
FROM pos p
         JOIN latest_px l ON l.symbol = p.symbol
WHERE p.net_qty <> 0
ORDER BY p.account_id, p.symbol;

-- ============================================
-- 6. Account portfolio: market value + total P&L
-- ============================================
-- Roll positions up to the account: market value, realized, unrealized, total.
WITH latest_px AS (
    SELECT DISTINCT ON (symbol) symbol, close
    FROM market_data
    ORDER BY symbol, trade_date DESC
),
     pos AS (
         SELECT account_id,
                symbol,
                SUM(CASE WHEN side = 'buy'  THEN qty ELSE -qty END)     AS net_qty,
                SUM(CASE WHEN side = 'buy'  THEN qty ELSE 0 END)        AS buy_qty,
                SUM(CASE WHEN side = 'buy'  THEN qty * price ELSE 0 END) AS buy_cost,
                SUM(CASE WHEN side = 'sell' THEN qty ELSE 0 END)        AS sell_qty,
                SUM(CASE WHEN side = 'sell' THEN qty * price ELSE 0 END) AS sell_proceeds
         FROM fills
         GROUP BY account_id, symbol
     ),
     pnl AS (
         SELECT p.account_id,
                SUM(p.net_qty * l.close)                               AS market_value,
                SUM(p.sell_proceeds - p.sell_qty * (p.buy_cost / NULLIF(p.buy_qty, 0))) AS realized_pnl,
                SUM(p.net_qty * (l.close - p.buy_cost / NULLIF(p.buy_qty, 0)))          AS unrealized_pnl
         FROM pos p
                  JOIN latest_px l ON l.symbol = p.symbol
         GROUP BY p.account_id
     )
SELECT a.account_id,
       a.name,
       ROUND(pnl.market_value, 2)                                     AS market_value,
       ROUND(pnl.realized_pnl, 2)                                     AS realized_pnl,
       ROUND(pnl.unrealized_pnl, 2)                                   AS unrealized_pnl,
       ROUND(pnl.realized_pnl + pnl.unrealized_pnl, 2)                AS total_pnl
FROM trading_accounts a
         JOIN pnl ON pnl.account_id = a.account_id
ORDER BY total_pnl DESC;

-- ============================================
-- 7. Order fill rate (filled qty / ordered qty)
-- ============================================
-- LEFT JOIN so canceled / working orders with no fills still show up at 0%.
SELECT o.order_id,
       o.account_id,
       o.symbol,
       o.side,
       o.ordered_qty,
       COALESCE(SUM(f.qty), 0)                                        AS filled_qty,
       ROUND(100.0 * COALESCE(SUM(f.qty), 0) / NULLIF(o.ordered_qty, 0), 1) AS fill_rate_pct,
       CASE
           WHEN COALESCE(SUM(f.qty), 0) = 0        THEN 'unfilled'
           WHEN SUM(f.qty) < o.ordered_qty         THEN 'partial'
           ELSE 'filled'
       END                                                           AS fill_state
FROM trade_orders o
         LEFT JOIN fills f ON f.order_id = o.order_id
GROUP BY o.order_id, o.account_id, o.symbol, o.side, o.ordered_qty
ORDER BY fill_rate_pct, o.order_id;

-- ============================================
-- 8. Daily returns + worst single day per symbol
-- ============================================
-- LAG for the prior close; MIN(...) OVER (PARTITION BY symbol) for the worst day.
WITH rtn AS (
    SELECT symbol,
           trade_date,
           close,
           LAG(close) OVER (PARTITION BY symbol ORDER BY trade_date) AS prev_close
    FROM market_data
)
SELECT symbol,
       trade_date,
       close,
       ROUND(100.0 * (close - prev_close) / NULLIF(prev_close, 0), 2) AS daily_return_pct,
       ROUND(MIN(100.0 * (close - prev_close) / NULLIF(prev_close, 0))
                 OVER (PARTITION BY symbol), 2)                       AS worst_day_pct
FROM rtn
ORDER BY symbol, trade_date;

-- True peak-to-trough drawdown: compare each close to the running high so far.
WITH mark AS (
    SELECT symbol,
           trade_date,
           close,
           MAX(close) OVER (PARTITION BY symbol ORDER BY trade_date
                            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_peak
    FROM market_data
)
SELECT symbol,
       trade_date,
       close,
       running_peak,
       ROUND(100.0 * (close - running_peak) / running_peak, 2)       AS drawdown_pct,
       ROUND(MIN(100.0 * (close - running_peak) / running_peak)
                 OVER (PARTITION BY symbol), 2)                       AS max_drawdown_pct
FROM mark
ORDER BY symbol, trade_date;

-- ============================================
-- 9. Most-traded symbols by notional + share of total
-- ============================================
-- notional = SUM(qty * price). SUM(SUM(...)) OVER () = grand total over the grouped rows.
SELECT symbol,
       SUM(qty)                                                      AS shares,
       ROUND(SUM(qty * price), 2)                                    AS notional,
       ROUND(100.0 * SUM(qty * price) / SUM(SUM(qty * price)) OVER (), 2) AS pct_of_total,
       RANK() OVER (ORDER BY SUM(qty * price) DESC)                  AS notional_rank
FROM fills
GROUP BY symbol
ORDER BY notional DESC;

-- ============================================
-- 10. Self-cross / wash-trade detection (self-join)
-- ============================================
-- Same account buys and sells the same symbol, same size, ~same price,
-- within a few seconds — a classic surveillance flag.
SELECT b.account_id,
       b.symbol,
       b.fill_id                                                     AS buy_fill,
       s.fill_id                                                     AS sell_fill,
       b.qty,
       b.price                                                       AS buy_price,
       s.price                                                       AS sell_price,
       s.filled_at - b.filled_at                                     AS gap
FROM fills b
         JOIN fills s
              ON s.account_id = b.account_id
                  AND s.symbol = b.symbol
                  AND b.side = 'buy' AND s.side = 'sell'
                  AND s.qty = b.qty
                  AND s.filled_at BETWEEN b.filled_at
                                      AND b.filled_at + INTERVAL '5 seconds'  -- within 5s
                  AND ABS(s.price - b.price) <= 0.10                          -- price barely moved
ORDER BY b.account_id, b.symbol;

-- ============================================
-- Bonus. LeetCode 1393 - Capital Gain/Loss
-- ============================================
-- Stocks(stock_name, operation ['Buy'|'Sell'], price) — one row per transaction.
-- Sell adds cash, Buy removes it → signed SUM per stock.
SELECT stock_name,
       SUM(CASE WHEN operation = 'Sell' THEN price ELSE -price END)  AS capital_gain_loss
FROM Stocks
GROUP BY stock_name;
