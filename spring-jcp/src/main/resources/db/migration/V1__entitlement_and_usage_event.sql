-- One row per (customer, feature): the quota they bought and how much they have used.
CREATE TABLE entitlement (
    customer_id TEXT   NOT NULL,
    feature     TEXT   NOT NULL,
    quota       BIGINT NOT NULL CHECK (quota >= 0),
    used        BIGINT NOT NULL DEFAULT 0 CHECK (used >= 0),
    PRIMARY KEY (customer_id, feature)
);

-- Every accepted usage event. event_key is the idempotency key.
CREATE TABLE usage_event (
    event_key   TEXT        PRIMARY KEY,
    customer_id TEXT        NOT NULL,
    user_id     TEXT        NOT NULL,
    feature     TEXT        NOT NULL,
    units       BIGINT      NOT NULL CHECK (units > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
