package com.richieloco.coinsniper.config;

import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestConfiguration
public class CoinSniperMockTestConfig {

    @Bean
    public WebClient testWebClient() {
        return WebClient.create();
    }

    @Bean
    public CoinSniperConfig coinSniperConfig() {
        // Top-level config
        CoinSniperConfig config = new CoinSniperConfig();

        // Supported section
        CoinSniperConfig.Supported supported = new CoinSniperConfig.Supported();
        supported.setExchanges(List.of("Binance", "Bybit"));
        supported.setStableCoins(List.of("USDT", "USDC"));
        config.setSupported(supported);

        // API section
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        CoinSniperConfig.Api.Binance.Announcement announcement = new CoinSniperConfig.Api.Binance.Announcement();
        announcement.setBaseUrl("http://mock-url.com");
        announcement.setType(1);
        announcement.setPageNo(1);
        announcement.setPageSize(10);
        binance.setAnnouncement(announcement);
        api.setBinance(binance);

        config.setApi(api);
        return config;
    }

    @Bean
    public ExchangeAssessor exchangeAssessor() {
        ExchangeAssessor mock = mock(ExchangeAssessor.class);

        ExchangeAssessmentRecord record = ExchangeAssessmentRecord.builder()
                .exchange("Binance")
                .coinListing("XYZ")
                .overallRiskScore(4)
                .liquidity("LOW")
                .tradingVolume("MEDIUM")
                .tradingFees("HIGH")
                .assessedAt(java.time.Instant.now())
                .build();

        when(mock.assess(any(ExchangeSelectorContext.class))).thenReturn(Mono.just(record));

        return mock;
    }
}
