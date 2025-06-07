package com.richieloco.coinsniper.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class CoinSniperMockTestConfig {

    @Bean
    public CoinSniperConfig coinSniperConfig() {
        CoinSniperConfig config = mock(CoinSniperConfig.class);
        CoinSniperConfig.Api api = mock(CoinSniperConfig.Api.class);
        CoinSniperConfig.Api.Binance binance = mock(CoinSniperConfig.Api.Binance.class);
        CoinSniperConfig.Api.Binance.Announcement announcement = new CoinSniperConfig.Api.Binance.Announcement();

        announcement.setBaseUrl("http://mock-url.com");
        announcement.setType(1);
        announcement.setPageNo(1);
        announcement.setPageSize(10);

        when(config.getApi()).thenReturn(api);
        when(api.getBinance()).thenReturn(binance);
        when(binance.getAnnouncement()).thenReturn(announcement);

        return config;
    }
}

