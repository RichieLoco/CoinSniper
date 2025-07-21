package com.richieloco.coinsniper.config;

import com.richieloco.coinsniper.config.CoinSniperConfig.Api.Binance.Announcement;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import com.richieloco.coinsniper.service.AnnouncementPollingScheduler;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
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
    public ChatModel chatModel() {
        return mock(ChatModel.class);
    }

    @Bean
    public AiPromptConfig aiPromptConfig() {
        AiPromptConfig config = mock(AiPromptConfig.class);
        PromptTemplate mockTemplate = mock(PromptTemplate.class);
        when(mockTemplate.render(anyMap())).thenReturn("Exchange: Binance, Coin Listing: XYZUSDT, Overall Risk Score: 3, Liquidity: HIGH, Trading Volume: MEDIUM, Trading Fees: LOW");

        when(config.exchangeCoinAvailabilityPromptTemplate()).thenReturn(mockTemplate);
        when(config.exchangeCoinRiskPromptTemplate()).thenReturn(mockTemplate);

        return config;
    }


    @Bean
    public CoinSniperConfig coinSniperConfig() {
        CoinSniperConfig config = new CoinSniperConfig();

        CoinSniperConfig.Supported supported = new CoinSniperConfig.Supported();
        supported.setExchanges(List.of("Binance", "Bybit"));
        supported.setStableCoins(List.of("USDT", "USDC"));
        config.setSupported(supported);

        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        Announcement announcement = new Announcement();
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
    public AnnouncementPollingConfig announcementPollingConfig() {
        AnnouncementPollingConfig config = new AnnouncementPollingConfig();
        config.setEnabled(true); // Make sure polling is enabled in test
        config.setIntervalSeconds(1); // Short interval
        return config;
    }

    @Bean
    public ExchangeAssessor exchangeAssessor() {
        ExchangeAssessor mock = mock(ExchangeAssessor.class);
        ExchangeAssessmentRecord record = ExchangeAssessmentRecord.builder()
                .exchange("Binance")
                .coinListing("XYZ")
                .overallRiskScore(3)
                .liquidity("HIGH")
                .tradingVolume("MEDIUM")
                .tradingFees("LOW")
                .assessedAt(java.time.Instant.now())
                .build();

        when(mock.assess(any(ExchangeSelectorContext.class))).thenReturn(Mono.just(record));
        return mock;
    }

    @Bean
    public AnnouncementCallingService announcementCallingService() {
        AnnouncementCallingService mock = mock(AnnouncementCallingService.class);

        // Return a Flux that emits dummy records slowly (so the polling doesn't terminate)
        CoinAnnouncementRecord dummy = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .title("New Coin XYZ listed")
                .build();

        when(mock.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.just(dummy).delayElements(Duration.ofMillis(300))); // simulate async delay

        return mock;
    }


    @Bean
    public AnnouncementPollingScheduler announcementPollingScheduler(
            AnnouncementCallingService service,
            AnnouncementPollingConfig config,
            CoinSniperConfig coinSniperConfig
    ) {
        return new AnnouncementPollingScheduler(service, config, coinSniperConfig);
    }

    @TestConfiguration
    public static class TestSecurityConfig {
        @Bean
        public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .authorizeExchange(spec -> spec.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .build();
        }
    }
}
