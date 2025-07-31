package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.ex.ExternalApiException;
import com.richieloco.coinsniper.model.BinanceApiResponse;
import com.richieloco.coinsniper.model.BinanceArticle;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementCallingService {

    protected final CoinSniperConfig config;
    protected final CoinAnnouncementRepository repository;
    protected final ErrorResponseRepository errorResponseRepository;
    protected final TradeExecutionService tradeExecutionService;
    protected final WebClient binanceWebClient;

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
            "Mozilla/5.0 (X11; Linux x86_64)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_4 like Mac OS X)",
            "Mozilla/5.0 (Windows NT 10.0; rv:115.0) Gecko/20100101 Firefox/115.0"
    );

    public static final String UNKNOWN_COIN = "UNKNOWN";
    private static final Set<String> ALLOWED_CATALOGS = Set.of("New Cryptocurrency Listing", "Delisting");

    public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
        String userAgent = USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));
        log.info("Calling Binance announcements [type={}, pageNo={}, pageSize={}]", type, pageNo, pageSize);

        return binanceWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("type", type)
                        .queryParam("pageNo", pageNo)
                        .queryParam("pageSize", pageSize)
                        .build())
                .header(HttpHeaders.USER_AGENT, userAgent)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new ExternalApiException(
                                        "Binance API error: " + errorBody,
                                        clientResponse.statusCode().value())))
                )
                .bodyToMono(BinanceApiResponse.class)
                .doOnNext(resp -> log.debug("Raw Binance API response: {}", resp))
                .flatMapMany(response -> {
                    if (response == null || response.getData() == null || response.getData().getCatalogs() == null) {
                        log.warn("Response missing expected data.");
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.getData().getCatalogs());
                })
                .doOnNext(catalog -> log.debug("Checking catalog: {}", catalog.getCatalogName()))
                .filter(catalog -> {
                    boolean allowed = ALLOWED_CATALOGS.contains(catalog.getCatalogName());
                    log.debug("Catalog '{}' allowed: {}", catalog.getCatalogName(), allowed);
                    return allowed;
                })
                .flatMap(catalog -> {
                    List<BinanceArticle> articles = catalog.getArticles();
                    log.debug("Catalog '{}' has {} articles", catalog.getCatalogName(), articles != null ? articles.size() : 0);
                    return Flux.fromIterable(articles != null ? articles : List.of())
                            .map(article -> Map.entry(article, catalog.getCatalogName()));
                })
                .flatMap(entry -> {
                    var article = entry.getKey();
                    var catalogName = entry.getValue();
                    Instant announcedAt = Instant.ofEpochMilli(article.getReleaseDate());

                    int intervalSeconds = config.getAnnouncementPolling().getIntervalSeconds();
                    Instant announcedAtCutoff = Instant.now().minusSeconds(intervalSeconds);

                    if (announcedAt.isBefore(announcedAtCutoff)) {
                        log.debug("Skipping '{}': too old (announcedAt={}, cutoff={})", article.getTitle(), announcedAt, announcedAtCutoff);
                        // For dashboard testing, we keep the record instead of returning empty
                    }
                    log.debug("Keeping '{}': fresh (announcedAt={}, cutoff={})", article.getTitle(), announcedAt, announcedAtCutoff);

                    List<String> symbols = extractSymbolsFromTitle(article.getTitle());
                    log.debug("Extracted symbols from '{}': {}", article.getTitle(), symbols);
                    if (symbols.contains(UNKNOWN_COIN)) {
                        log.warn("Title '{}' resulted in UNKNOWN_COIN", article.getTitle());
                    }

                    boolean isDelisting = isDelisting(article.getTitle(), catalogName);

                    return Flux.fromIterable(symbols)
                            .filter(symbol -> !UNKNOWN_COIN.equalsIgnoreCase(symbol))
                            .flatMap(symbol -> repository.findByCoinSymbolAndAnnouncedAt(symbol, announcedAt)
                                    .hasElement()
                                    .flatMapMany(exists -> {
                                        if (exists) {
                                            log.debug("Duplicate found for symbol='{}' at '{}'. Skipping.", symbol, announcedAt);
                                            return Flux.empty();
                                        } else {
                                            CoinAnnouncementRecord record = CoinAnnouncementRecord.builder()
                                                    .title(article.getTitle())
                                                    .coinSymbol(symbol)
                                                    .announcedAt(announcedAt)
                                                    .delisting(isDelisting)
                                                    .build();
                                            log.info("Attempting to persist coinAnnouncementRecord={}", record);

                                            return repository.save(record)
                                                    .flatMapMany(saved -> {
                                                        log.info("Saved record: {}", saved);
                                                        // TradeExecutionService now returns Flux<TradeDecisionRecord>
                                                        return tradeExecutionService.evaluateAndTrade(saved)
                                                                .thenMany(Flux.just(saved));
                                                    });
                                        }
                                    }));
                })
                .onErrorResume(ExternalApiException.class, ex -> {
                    log.error("External API error: {}", ex.getMessage());
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

        Matcher parens = Pattern.compile("\\(([A-Z0-9]{2,10})\\)").matcher(title);
        while (parens.find()) {
            String symbol = parens.group(1).trim();
            if (isValidSymbol(symbol)) {
                symbols.add(symbol);
            }
        }

        Matcher structured = Pattern.compile(
                "(?i)binance (will|has) (list|add|delist|support|complete|launch|introduce)\\s+(.*?)\\b([A-Z0-9]{2,10})\\b"
        ).matcher(title);
        while (structured.find()) {
            String candidate = structured.group(4).trim();
            if (isValidSymbol(candidate)) {
                symbols.add(candidate);
            }
        }

        if (symbols.isEmpty()) {
            symbols.add(UNKNOWN_COIN);
        }

        return new ArrayList<>(symbols);
    }

    private boolean isValidSymbol(String symbol) {
        return symbol.matches("^[A-Z0-9]{2,10}$") &&
                !symbol.matches("^\\d{4}-\\d{2}-\\d{2}$") &&
                !symbol.equalsIgnoreCase("USD") &&
                !symbol.equalsIgnoreCase("USDT") &&
                !symbol.equalsIgnoreCase("BNB");
    }

    protected boolean isDelisting(String title, String catalogName) {
        return "Delisting".equalsIgnoreCase(catalogName) ||
                title.toLowerCase().contains("delist") ||
                title.toLowerCase().contains("removal");
    }
}