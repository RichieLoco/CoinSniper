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
    coin_symbol VARCHAR(255),
    exchange VARCHAR(255),
    risk_score DOUBLE PRECISION,
    trade_executed BOOLEAN,
    timestamp TIMESTAMP
);

DROP TABLE IF EXISTS error_responses;

CREATE TABLE error_responses (
    id UUID PRIMARY KEY,
    source VARCHAR(255),
    error_message VARCHAR(1000),
    status_code INT,
    timestamp TIMESTAMP
);
