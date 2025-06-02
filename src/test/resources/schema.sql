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
