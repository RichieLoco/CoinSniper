package com.richieloco.coinsniper.repo;

import com.richieloco.coinsniper.entity.on.log.RiskAssessmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentLogRepository extends JpaRepository<RiskAssessmentLog, Long> {
}
