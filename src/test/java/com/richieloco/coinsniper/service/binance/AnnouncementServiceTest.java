package com.richieloco.coinsniper.service.binance;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.Announcement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnnouncementServiceTest {

    private CoinSniperConfig apiConfig;
    private AnnouncementService service;

    private WebClient mockWebClient;
    private WebClient.RequestHeadersUriSpec uriSpecMock;
    private WebClient.RequestHeadersSpec<?> headersSpecMock;
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        mockWebClient = mock(WebClient.class);
        uriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        headersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        responseSpecMock = mock(WebClient.ResponseSpec.class);

        apiConfig = mock(CoinSniperConfig.class);
        CoinSniperConfig.BinanceConfig config = mock(CoinSniperConfig.BinanceConfig.class);

        // Return base URL
        when(apiConfig.getBinance()).thenReturn(config);
        when(config.getBaseUrl()).thenReturn("/bapi/composite/v1/public/cms/article/catalog/list/query");

        // 🔧 Break the chain manually
        when(mockWebClient.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(any(Function.class))).thenAnswer(invocation -> headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        service = new AnnouncementService(mockWebClient, apiConfig);
    }


    @Test
    public void testFetchAnnouncements_Success() {
        Announcement mockResponse = new Announcement();
        when(responseSpecMock.bodyToMono(Announcement.class)).thenReturn(Mono.just(mockResponse));

        Mono<Announcement> result = service.fetchAnnouncements("1", "1", "10");

        StepVerifier.create(result)
                .expectNext(mockResponse)
                .verifyComplete();

        verify(headersSpecMock).retrieve();
    }

    @Test
    public void testFetchAnnouncements_ClientError() {
        when(responseSpecMock.bodyToMono(Announcement.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        null,
                        null,
                        null)));

        Mono<Announcement> result = service.fetchAnnouncements("1", "bad", "10");

        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    public void testFetchAnnouncements_TimeoutOrNetworkError() {
        when(responseSpecMock.bodyToMono(Announcement.class))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

        Mono<Announcement> result = service.fetchAnnouncements("1", "1", "10");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("timeout"))
                .verify();
    }

    @Test
    public void testFetchAnnouncements_NullResponse() {
        when(responseSpecMock.bodyToMono(Announcement.class)).thenReturn(Mono.empty());

        Mono<Announcement> result = service.fetchAnnouncements("1", "1", "10");

        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }
}
