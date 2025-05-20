package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface ExchangeAssessmentRepository extends ReactiveCrudRepository<ExchangeAssessmentRecord, UUID> {
}
