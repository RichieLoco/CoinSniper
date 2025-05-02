package com.richieloco.coinsniper.handler;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.Announcement;
import com.richieloco.coinsniper.service.binance.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

public class AnnouncementHandlerTest {

    private AnnouncementService announcementService;
    private CoinSniperConfig.BinanceConfig config;
    private AnnouncementHandler handler;

    @BeforeEach
    void setUp() {
        announcementService = Mockito.mock(AnnouncementService.class);
        config = new CoinSniperConfig.BinanceConfig();
        config.setType(1);
        config.setPageNo(1);
        config.setPageSize(10);

        handler = new AnnouncementHandler(announcementService, config);
    }

    @Test
    void getAnnouncements_withParams_shouldCallService() {
        ServerRequest request = Mockito.mock(ServerRequest.class);

        Mockito.when(request.queryParam("type")).thenReturn(Optional.of("1"));
        Mockito.when(request.queryParam("pageNo")).thenReturn(Optional.of("2"));
        Mockito.when(request.queryParam("pageSize")).thenReturn(Optional.of("20"));

        Announcement mockAnnouncement = new Announcement();
        Mockito.when(announcementService.fetchAnnouncements("1", "2", "20"))
                .thenReturn(Mono.just(mockAnnouncement));

        StepVerifier.create(handler.getAnnouncements(request))
                .expectNextMatches(response -> response.statusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void getAnnouncements_missingParams_shouldUseConfigDefaults() {
        ServerRequest request = Mockito.mock(ServerRequest.class);

        Mockito.when(request.queryParam("type")).thenReturn(Optional.empty());
        Mockito.when(request.queryParam("pageNo")).thenReturn(Optional.empty());
        Mockito.when(request.queryParam("pageSize")).thenReturn(Optional.empty());

        Announcement mockAnnouncement = new Announcement();
        Mockito.when(announcementService.fetchAnnouncements("1", "1", "10"))
                .thenReturn(Mono.just(mockAnnouncement));

        StepVerifier.create(handler.getAnnouncements(request))
                .expectNextMatches(response -> response.statusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void getAnnouncements_serviceReturnsEmpty_shouldReturnNoContent() {
        ServerRequest request = Mockito.mock(ServerRequest.class);

        Mockito.when(request.queryParam("type")).thenReturn(Optional.of("1"));
        Mockito.when(request.queryParam("pageNo")).thenReturn(Optional.of("1"));
        Mockito.when(request.queryParam("pageSize")).thenReturn(Optional.of("10"));

        Mockito.when(announcementService.fetchAnnouncements(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(handler.getAnnouncements(request))
                .expectNextMatches(response -> response.statusCode().value() == 204)
                .verifyComplete();
    }

    @Test
    void getAnnouncements_serviceThrowsException_shouldError() {
        ServerRequest request = Mockito.mock(ServerRequest.class);

        Mockito.when(request.queryParam("type")).thenReturn(Optional.of("1"));
        Mockito.when(request.queryParam("pageNo")).thenReturn(Optional.of("1"));
        Mockito.when(request.queryParam("pageSize")).thenReturn(Optional.of("10"));

        Mockito.when(announcementService.fetchAnnouncements(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));

        StepVerifier.create(handler.getAnnouncements(request))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Service failure"))
                .verify();
    }
}
