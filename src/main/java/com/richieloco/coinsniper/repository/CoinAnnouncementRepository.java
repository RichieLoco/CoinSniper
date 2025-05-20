package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CoinAnnouncementRepository extends ReactiveCrudRepository<CoinAnnouncementRecord, String> {
}
