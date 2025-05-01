package com.richieloco.coinsniper.router;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.Announcement;
import com.richieloco.coinsniper.handler.AnnouncementHandler;
import com.richieloco.coinsniper.service.binance.AnnouncementService;
import com.richieloco.coinsniper.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AnnouncementRouter.class, excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class
})
@Import({AnnouncementRouter.class, AnnouncementHandler.class, AnnouncementRouterTest.MockedBeans.class})
public class AnnouncementRouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AnnouncementService service;

    @Autowired
    private CoinSniperConfig.BinanceConfig config;

    @TestConfiguration
    static class MockedBeans {

        @Bean
        public AnnouncementService mockAnnouncementService() {
            return Mockito.mock(AnnouncementService.class);
        }

        @Bean
        public CoinSniperConfig.BinanceConfig mockBinanceConfig() {
            CoinSniperConfig.BinanceConfig mock = Mockito.mock(CoinSniperConfig.BinanceConfig.class);
            Mockito.when(mock.getType()).thenReturn(1);
            Mockito.when(mock.getPageNo()).thenReturn(1);
            Mockito.when(mock.getPageSize()).thenReturn(10);
            return mock;
        }
    }

    @Test
    void testGetAnnouncements() {
        Announcement announcement = TestUtil.readFromFile("testResponse_Full.json", Announcement.class);
        Assertions.assertNotNull(announcement);

        Mockito.when(service.fetchAnnouncements("1", "1", "10"))
                .thenReturn(Mono.just(announcement));

        webTestClient.get()
                .uri("/api/announcements/fetch?type=1&pageNo=1&pageSize=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Announcement.class)
                .isEqualTo(announcement);
    }
}
