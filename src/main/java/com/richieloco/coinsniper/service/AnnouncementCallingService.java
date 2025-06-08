package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.ex.ExternalApiException;
import com.richieloco.coinsniper.model.BinanceApiResponse;
import com.richieloco.coinsniper.model.BinanceCatalog;
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

    protected final CoinSniperConfig config;
    protected final CoinAnnouncementRepository repository;
    protected final ErrorResponseRepository errorResponseRepository;

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
                .bodyToMono(BinanceApiResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.getData().getCatalogs()))
                .flatMapIterable(BinanceCatalog::getArticles)
                .map(article -> CoinAnnouncementRecord.builder()
                        .title(article.getTitle())
                        .coinSymbol(extractSymbolFromTitle(article.getTitle()))
                        .announcedAt(Instant.ofEpochMilli(article.getReleaseDate()))
                        .delisting(isDelisting(article.getTitle()))
                        .build())
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

    protected String extractSymbolFromTitle(String title) {
        // TODO a very naive symbol extractor;... may replace with smarter regex or API mapping
        // e.g., "Binance Will List Bubblemaps (BMT)" => BMT
        if (title.contains("(") && title.contains(")")) {
            return title.substring(title.indexOf('(') + 1, title.indexOf(')')).trim();
        }
        return "UNKNOWN";
    }

    protected boolean isDelisting(String title) {
        return title.toLowerCase().contains("delist") || title.toLowerCase().contains("removal");
    }
}
