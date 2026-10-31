-- Running total right after this event was counted, so a duplicate can replay the original result.
-- Nullable on purpose: rows written before this migration have no value, and adding a nullable
-- column without a default is a metadata-only change in Postgres (no table rewrite, no long lock).
ALTER TABLE usage_event ADD COLUMN used_after BIGINT;
