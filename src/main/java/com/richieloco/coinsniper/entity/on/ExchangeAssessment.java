package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exchange_assessments")
public class ExchangeAssessment {
    private String exchange;
    private String coinListing;
    private Integer overallRiskScore;
    private RiskLevel tradingFees;
    private RiskLevel tradingVolume;
    private RiskLevel liquidity;

    @Override
    public String toString() {
        return "ExchangeAssessment{" +
                "exchange='" + exchange + '\'' +
                ", coinListing='" + coinListing + '\'' +
                ", overallRiskScore=" + overallRiskScore +
                ", tradingFees=" + tradingFees +
                ", tradingVolume=" + tradingVolume +
                ", liquidity=" + liquidity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeAssessment risk = (ExchangeAssessment) o;
        return Objects.equals(exchange, risk.exchange) && Objects.equals(coinListing, risk.coinListing) && Objects.equals(overallRiskScore, risk.overallRiskScore) && tradingFees == risk.tradingFees && tradingVolume == risk.tradingVolume && liquidity == risk.liquidity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, coinListing, overallRiskScore, tradingFees, tradingVolume, liquidity);
    }
}
