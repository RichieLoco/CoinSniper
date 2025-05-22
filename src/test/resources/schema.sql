CREATE TABLE trade_decisions (
    id UUID PRIMARY KEY,
    coin_symbol VARCHAR(50),
    exchange VARCHAR(50),
    risk_score DOUBLE,
    trade_executed BOOLEAN,
    timestamp TIMESTAMP
);