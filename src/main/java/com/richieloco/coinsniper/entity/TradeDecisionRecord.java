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
@Table("trade_decisions")
public class TradeDecisionRecord implements Identifiable {
    @Id
    @GeneratedValue
    private UUID id;
    private String coinSymbol;
    private String exchange;
    private double riskScore;
    private boolean tradeExecuted;
    private Instant timestamp;
}
