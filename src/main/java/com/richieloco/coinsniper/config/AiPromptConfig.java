package com.richieloco.coinsniper.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPromptConfig {

    /* Takes the full list of exchanges that can be traded upon to determine whether
       the announced coin exists on it and can be traded with a provided stable coin.
     */
    @Bean
    public PromptTemplate exchangeCoinAvailabilityPromptTemplate() {
        String template = """
        Given the following:
        
        - Exchanges: <exchanges>
        - Stable coins: <stableCoins>
        - Target coin: <targetCoin>
        
        Return a JSON array where each object describes a listing of the target coin traded against each supported stable coin on each regulated exchange that lists it.

        Do not include any explanation or text before or after the JSON.

        Use this strict format:
        [
          {{
            "exchange": "GateIo",
            "coinListing": "<targetCoin>/<stableCoin>",
            "overallRiskScore": "Medium",
            "liquidity": "High",
            "tradingVolume": "Medium",
            "tradingFees": "Low"
          }}
        ]
        
        Only include exchanges that list <targetCoin> with at least one stable coin from the list.
        Use exactly one object per (exchange, stable coin) pair.
        """;
        return new PromptTemplate(template);
    }
}
