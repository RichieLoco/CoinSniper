package com.richieloco.coinsniper.service.risk;

@FunctionalInterface
public interface RiskAssessmentFunction<T> {
    /**
     * Assesses the risk of a given trading action or context.
     * @param context the trading context input (e.g., trading action, exchange details, coin details)
     * @return a risk score from 0.0 (no risk) to 1.0 (maximum risk)
     */
    double assessRisk(T context);
}