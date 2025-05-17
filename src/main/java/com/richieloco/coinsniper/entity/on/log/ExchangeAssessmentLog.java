package com.richieloco.coinsniper.entity.on.log;

import com.richieloco.coinsniper.entity.on.ExchangeAssessment;
import com.richieloco.coinsniper.entity.on.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Entity
@Table(name = "exchange_assessment_log")
public class ExchangeAssessmentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contextType;
    private String contextDescription;
    private String exchange;
    private String coinListing;
    private Integer overallRiskScore;
    private RiskLevel tradingVolumeRiskLevel;
    private RiskLevel tradingLiquidityRiskLevel;
    private RiskLevel tradingFeesRiskLevel;
    private Instant assessedAt;
}
