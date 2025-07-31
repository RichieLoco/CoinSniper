package com.richieloco.coinsniper.entity;

import jakarta.persistence.GeneratedValue;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exchange_assessments")
public class ExchangeAssessmentRecord implements Identifiable {
    @Id
    @GeneratedValue
    private UUID id;
    private String contextType;
    private String contextDescription;
    private String exchange;
    private String coinListing;
    private String overallRiskScore;
    private String tradingVolume;
    private String liquidity;
    private String tradingFees;
    private Instant assessedAt;
}
