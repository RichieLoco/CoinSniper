package com.richieloco.coinsniper.config;

import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public CoinSniperConfig.BinanceConfig binanceConfig() {
        CoinSniperConfig.BinanceConfig config = new CoinSniperConfig.BinanceConfig();
        config.setBaseUrl("https://test-binance.com");
        return config;
    }

    @Bean
    public CoinSniperConfig coinSniperConfig(CoinSniperConfig.BinanceConfig binanceConfig) {
        CoinSniperConfig config = new CoinSniperConfig();
        config.setBinance(binanceConfig);
        return config;
    }

    // mock/stub beans here:
    @Bean
    public RiskAssessmentLogRepository riskAssessmentLogRepository() {
        return org.mockito.Mockito.mock(RiskAssessmentLogRepository.class);
    }
}
