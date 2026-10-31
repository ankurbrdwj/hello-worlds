-- Reconciliation sums usage_event.units per (customer_id, feature); without this it scans the whole table
-- for every entitlement. Plain CREATE INDEX takes a write lock on usage_event while it builds, which is fine
-- at this size; on a large live table use CREATE INDEX CONCURRENTLY (outside a Flyway transaction) instead.
CREATE INDEX usage_event_customer_feature_idx ON usage_event (customer_id, feature);
