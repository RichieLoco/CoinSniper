package com.richieloco.coinsniper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExchangeAssessmentResponse(
        @JsonProperty("exchange") String exchange,
        @JsonProperty("coinListing") String coinListing,
        @JsonProperty("overallRiskScore") String overallRiskScore,
        @JsonProperty("liquidity") String liquidity,
        @JsonProperty("tradingVolume") String tradingVolume,
        @JsonProperty("tradingFees") String tradingFees
) {}