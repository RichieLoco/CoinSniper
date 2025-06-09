package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface CoinAnnouncementRepository extends ReactiveCrudRepository<CoinAnnouncementRecord, String> {
    Mono<CoinAnnouncementRecord> findByCoinSymbolAndAnnouncedAt(String coinSymbol, Instant announcedAt);
}
