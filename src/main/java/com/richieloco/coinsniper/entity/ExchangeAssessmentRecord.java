package com.richieloco.coinsniper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "exchange_assessments")
public class ExchangeAssessmentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String contextType;
    private String contextDescription;
    private String exchange;
    private String coinListing;
    private Integer overallRiskScore;
    private RiskLevel tradingVolume;
    private RiskLevel liquidity;
    private RiskLevel tradingFees;
    private Instant assessedAt;
}