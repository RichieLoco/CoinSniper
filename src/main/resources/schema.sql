DROP TABLE IF EXISTS coin_announcements;

CREATE TABLE coin_announcements (
  id UUID PRIMARY KEY,
  title VARCHAR(255),
  coin_symbol VARCHAR(100),
  announced_at TIMESTAMP,
  delisting BOOLEAN
);

DROP TABLE IF EXISTS trade_decisions;

CREATE TABLE trade_decisions (
    id UUID PRIMARY KEY,
    coin_symbol VARCHAR(255) NOT NULL,
    exchange VARCHAR(255) NOT NULL,
    risk_score DOUBLE PRECISION,
    trade_executed BOOLEAN,
    decided_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ts_minute VARCHAR(16) NOT NULL
);

-- Enforce uniqueness per (coin, exchange, minute)
CREATE UNIQUE INDEX uq_trade_decisions_coin_exchange_per_minute
  ON trade_decisions (coin_symbol, exchange, ts_minute);

DROP TABLE IF EXISTS error_responses;

CREATE TABLE error_responses (
    id UUID PRIMARY KEY,
    source VARCHAR(255),
    error_message VARCHAR(1000),
    status_code INT,
    "timestamp" TIMESTAMP
);

DROP TABLE IF EXISTS exchange_assessments;

CREATE TABLE exchange_assessments (
  id UUID PRIMARY KEY,
  context_type VARCHAR(255),
  context_description VARCHAR(255),
  exchange VARCHAR(255),
  coin_listing VARCHAR(255),
  overall_risk_score VARCHAR(255),
  trading_volume VARCHAR(255),
  liquidity VARCHAR(255),
  trading_fees VARCHAR(255),
  assessed_at TIMESTAMP
);
