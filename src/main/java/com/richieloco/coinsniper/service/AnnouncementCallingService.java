package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AnnouncementCallingService {

    private final CoinSniperConfig config;
    private final CoinAnnouncementRepository repository;
    private final ErrorResponseRepository errorResponseRepository;

    private final WebClient webClient = WebClient.create();

    public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
        // Construct the URI with query parameters
        URI uri = UriComponentsBuilder.fromUriString(config.getApi().getBinance().getAnnouncement().getBaseUrl())
                .queryParam("type", type)
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .build()
                .toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new ExternalApiException(
                                        "Binance API error: " + errorBody,
                                        clientResponse.statusCode().value())))
                )
                .bodyToMono(String.class)
                .flatMapMany(body -> Flux.just(
                        CoinAnnouncementRecord.builder()
                                .title("Example Coin Listing")
                                .coinSymbol("XYZ")
                                .announcedAt(Instant.now())
                                .delisting(false)
                                .build()
                ))
                .flatMap(repository::save)
                .onErrorResume(ExternalApiException.class, ex -> {
                    ErrorResponseRecord error = ErrorResponseRecord.builder()
                            .source("Binance")
                            .errorMessage(ex.getMessage())
                            .statusCode(ex.getStatusCode())
                            .timestamp(Instant.now())
                            .build();
                    return errorResponseRepository.save(error).thenMany(Flux.empty());
                });
    }

    @Getter
    @SuppressWarnings("serial")
    private static class ExternalApiException extends RuntimeException {
        private final int statusCode;

        public ExternalApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

    }
}
