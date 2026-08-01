-- Initialize market database schema
CREATE TABLE IF NOT EXISTS market_quotes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticker       VARCHAR(10) NOT NULL,
    price        NUMERIC(18,4) NOT NULL,
    change_pct   NUMERIC(18,4) NOT NULL,
    volume       BIGINT NOT NULL,
    timestamp    TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quotes_ticker ON market_quotes (ticker);
CREATE INDEX IF NOT EXISTS idx_quotes_timestamp ON market_quotes (timestamp DESC);

CREATE TABLE IF NOT EXISTS processed_quotes (
    id          SERIAL PRIMARY KEY,
    ticker      VARCHAR(10) NOT NULL,
    price       NUMERIC(18,4),
    change_pct  NUMERIC(18,4),
    volume      BIGINT,
    ts          TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sentiment_scores (
    id         SERIAL PRIMARY KEY,
    ticker     VARCHAR(10) NOT NULL,
    sentiment  VARCHAR(20),
    score      NUMERIC(6,4),
    source     VARCHAR(50),
    ts         TIMESTAMPTZ DEFAULT NOW()
);
