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
                Given exchanges... {exchanges}... return to me a comma-delimited list (with no descriptions) of the regulated cryptocurrency
                exchanges that list the coin {targetCoin} that can be traded with stable coin(s) {stableCoins} delimiting each exchange and
                stable coin with a colon(:) and delimiting each supported stable coin with a forward-slash(/).
                """;
        return new PromptTemplate(template); // e.g. Poloniex:USDT, Bybit:USDT/USDC, KuCoin:USDT, OKX:USDC
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
        // e.g. Exchange: Poloniex, Coin Listing: WIFUSDT, Overall Risk Score: 7, Liquidity: Low, Trading Volume: Low, Trading Fees: Low
    }

    //TODO the below have been superseded by the above, and can be removed...

    /* Takes the list of exchanges of which the coin exists, and determines the difference
        in latency when trading on that exchange versus the others, and gives and overall
        risk score.
     */
    @Bean
    public PromptTemplate exchangeLatencyDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to exchange latency difference when trading using the respective exchanges' APIs, where the lower the score, the less volatile the
            market. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    /* Takes the list of exchanges of which the coin exists, and determines the difference
        in liquidity when trading on that exchange versus the others, and gives and overall
        risk score.
     */
    @Bean
    public PromptTemplate exchangeLiquidityDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to exchange liquidity, where the lower the score, the more liquidity the
            market. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }


    /* Takes the list of exchanges of which the coin exists, and determines the difference
        in fees when trading on that exchange versus the others, and gives an overall
        risk score.
     */
    @Bean
    public PromptTemplate exchangeFeeDifferencePromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            with respect to trading fees difference, where the lower the score, the less the trading
            fees for that exchange. Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    //TODO might have been superseded by the above prompts
    @Bean
    public PromptTemplate exchangeRiskPromptTemplate() {
        String template = """
            Assess the risk (0.0 to 1.0) of trading from exchange {fromExchange} to {toExchange}
            given the market volatility of {marketVolatility}.
            Respond with only the number.
            """;
        return new PromptTemplate(template);
    }

    //TODO might not be needed...
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
