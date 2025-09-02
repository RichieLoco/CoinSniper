package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface TradeDecisionRepository extends ReactiveCrudRepository<TradeDecisionRecord, UUID> {

    Mono<TradeDecisionRecord> findTopByCoinSymbolOrderByDecidedAtDesc(String coinSymbol);

    @Query("""
MERGE INTO trade_decisions (id, coin_symbol, exchange, risk_score, trade_executed, decided_at, ts_minute)
KEY (coin_symbol, exchange, ts_minute)
VALUES (:id, :coinSymbol, :exchange, :riskScore, :tradeExecuted, :decidedAt, :tsMinute)
""")
    Mono<Void> upsertPerMinute(UUID id,
                               String coinSymbol,
                               String exchange,
                               Double riskScore,
                               Boolean tradeExecuted,
                               Instant decidedAt,
                               String tsMinute);
}