package com.richieloco.coinsniper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExchangeAssessmentResponse(
        @JsonProperty("Exchange") String exchange,
        @JsonProperty("Coin Listing") String coinListing,
        @JsonProperty("Overall Risk Score") int overallRiskScore,
        @JsonProperty("Liquidity") String liquidity,
        @JsonProperty("Trading Volume") String tradingVolume,
        @JsonProperty("Trading Fees") String tradingFees
) {}
