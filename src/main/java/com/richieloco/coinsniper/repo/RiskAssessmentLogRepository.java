package com.richieloco.coinsniper.repo;

import com.richieloco.coinsniper.entity.on.log.ExchangeAssessmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentLogRepository extends JpaRepository<ExchangeAssessmentLog, Long> {
}
