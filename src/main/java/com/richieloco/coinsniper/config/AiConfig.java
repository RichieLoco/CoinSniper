package com.richieloco.coinsniper.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

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
