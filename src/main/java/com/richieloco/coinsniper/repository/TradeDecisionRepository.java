package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface TradeDecisionRepository extends ReactiveCrudRepository<TradeDecisionRecord, UUID> {
}
