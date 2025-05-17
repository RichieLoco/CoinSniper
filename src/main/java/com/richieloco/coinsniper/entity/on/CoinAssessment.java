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
@Table(name = "coin_assessments")
public class CoinAssessment {
    private String coin;
    private Integer overallRiskScore;
    private RiskLevel marketSentiment;
    private RiskLevel volatility;
    private RiskLevel newsImpact;

    @Override
    public String toString() {
        return "CoinAssessment{" +
                "coin='" + coin + '\'' +
                ", overallRiskScore=" + overallRiskScore +
                ", marketSentiment=" + marketSentiment +
                ", volatility=" + volatility +
                ", newsImpact=" + newsImpact +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CoinAssessment that = (CoinAssessment) o;
        return Objects.equals(coin, that.coin) &&
                Objects.equals(overallRiskScore, that.overallRiskScore) &&
                marketSentiment == that.marketSentiment &&
                volatility == that.volatility &&
                newsImpact == that.newsImpact;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coin, overallRiskScore, marketSentiment, volatility, newsImpact);
    }
}
