package com.richieloco.coinsniper.service.risk;

import com.richieloco.coinsniper.entity.on.Risk;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface RiskAssessmentFunction<T> {
    /**
     * Assesses the risk of a given trading action or context.
     * @param context the trading context input (e.g., trading action, exchange details, coin details)
     * @return a risk score from 0.0 (no risk) to 1.0 (maximum risk)
     */
    Mono<Risk> assessRisk(T context);
}