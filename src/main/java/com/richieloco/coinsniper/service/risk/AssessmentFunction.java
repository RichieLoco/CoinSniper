package com.richieloco.coinsniper.service.risk;

import reactor.core.publisher.Mono;

/**
 * Generic assessment function interface that takes a context of type T and returns an assessment of type R.
 *
 * @param <T> the type of input context (e.g., exchange info, coin info)
 * @param <R> the type of assessment result (e.g., ExchangeAssessment, CoinAssessment)
 */
@FunctionalInterface
public interface AssessmentFunction<T, R> {
    /**
     * Assesses a given trading action or context.
     *
     * @param context the trading context input
     * @return a Mono emitting the risk assessment result
     */
    Mono<R> assess(T context);
}