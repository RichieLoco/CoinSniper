package com.richieloco.coinsniper.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant decidedAt;
    //... below used to eliminate duplicates from db
    private String tsMinute;

}
