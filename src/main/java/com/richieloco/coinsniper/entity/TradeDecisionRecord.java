package com.richieloco.coinsniper.entity;

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
public class TradeDecisionRecord {
    @Id
    private UUID id;
    private String coinSymbol;
    private String exchange;
    private double riskScore;
    private boolean tradeExecuted;
    private Instant timestamp;
}
