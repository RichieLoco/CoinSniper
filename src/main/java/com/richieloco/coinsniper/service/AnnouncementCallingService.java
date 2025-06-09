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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnnouncementCallingService {

    protected final CoinSniperConfig config;
    protected final CoinAnnouncementRepository repository;
    protected final ErrorResponseRepository errorResponseRepository;

    public static final String UNKNOWN_COIN = "UNKNOWN";

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
                .flatMap(article -> {
                    List<String> symbols = extractSymbolsFromTitle(article.getTitle());
                    return Flux.fromIterable(symbols)
                            .map(symbol -> CoinAnnouncementRecord.builder()
                                    .title(article.getTitle())
                                    .coinSymbol(symbol)
                                    .announcedAt(Instant.ofEpochMilli(article.getReleaseDate()))
                                    .delisting(isDelisting(article.getTitle()))
                                    .build());
                })
                .flatMap(repository::save)  // ✅ now properly chained
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

    protected List<String> extractSymbolsFromTitle(String title) {
        Set<String> symbols = new LinkedHashSet<>();

        // Match symbols in parentheses (e.g. (XUSD), (FORM))
        Matcher parens = Pattern.compile("\\(([A-Z0-9]{2,10})\\)").matcher(title);
        while (parens.find()) {
            String symbol = parens.group(1).trim();
            if (isValidSymbol(symbol)) {
                symbols.add(symbol);
            }
        }

        // Match structured listing/delisting/support phrases, but avoid generic marketing
        Matcher structured = Pattern.compile(
                "(?i)binance (will|has) (list|add|delist|support|complete|launch|introduce)\\s+(.*?)\\b([A-Z0-9]{2,10})\\b"
        ).matcher(title);
        while (structured.find()) {
            String candidate = structured.group(4).trim();
            if (isValidSymbol(candidate)) {
                symbols.add(candidate);
            }
        }

        // Fallback to "UNKNOWN" if nothing valid was found
        if (symbols.isEmpty()) {
            symbols.add(UNKNOWN_COIN);
        }

        return new ArrayList<>(symbols);
    }

    private boolean isValidSymbol(String symbol) {
        return symbol.matches("^[A-Z0-9]{2,10}$") &&
                !symbol.matches("^\\d{4}-\\d{2}-\\d{2}$") && // exclude dates
                !symbol.equalsIgnoreCase("USD") &&
                !symbol.equalsIgnoreCase("USDT") &&
                !symbol.equalsIgnoreCase("BNB"); // optional common tokens to skip
    }

    protected boolean isDelisting(String title) {
        return title.toLowerCase().contains("delist") || title.toLowerCase().contains("removal");
    }
}
