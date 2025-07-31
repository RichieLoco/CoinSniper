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
            "coinListing": "<targetCoin>/<a stable coin from stableCoins>",
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



    /* Takes the full list of exchanges that can be traded upon to determine whether
        the announced coin exists on it.
     */
    @Bean
    public PromptTemplate exchangeCoinRiskPromptTemplate() {
        String template = """
                Given exchange... {exchange} and corresponding coin listing... {listing}... return to me an overall risk score (the
                lesser the score, the better, with no descriptions) of trading this listing on this exchange, assessing the following
                risk factors: liquidity (more liquid, least risky), trading volume (more volume, less risk) and trading fees (the
                cheaper, the better) - returning just the overall risk score and the values of these factors in a colon (:) delimited
                list one-liner  - with format: "Exchange: ?, Coin Listing: ?, Overall Risk Score: ?, Liquidity: ?, Trading Volume: ?, Trading Fees: ?"
                """;
        return new PromptTemplate(template);
    }

    @Bean
    public PromptTemplate exchangeLatencyDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to exchange latency difference when trading using the respective exchanges' APIs, where the lower the score, the less volatile the
            market. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    @Bean
    public PromptTemplate exchangeLiquidityDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to exchange liquidity, where the lower the score, the more liquidity the
            market. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    @Bean
    public PromptTemplate exchangeFeeDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to trading fees difference, where the lower the score, the less the trading
            fees for that exchange. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    @Bean
    public PromptTemplate exchangeRiskPromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            given the market volatility of {marketVolatility}.
            Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    @Bean
    public PromptTemplate coinRiskPromptTemplate() {
        String template = """
            Evaluate the risk of trading {coinA} versus {coinB}.
            Consider historical volatility ({historicalVolatility}) and correlation ({correlation}).
            Return a number between 0.0 (no risk) and 1.0 (high risk).
            Respond with only the number.
            """;
        return new PromptTemplate(template);
    }
}
