package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TradeDecisionRepository extends ReactiveCrudRepository<TradeDecisionRecord, UUID> {

    Mono<TradeDecisionRecord> findTopByCoinSymbolOrderByTimestampDesc(String coinSymbol);
}
