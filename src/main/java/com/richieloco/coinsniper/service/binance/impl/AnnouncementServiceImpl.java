package com.richieloco.coinsniper.service.binance.impl;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.AnnouncementResponse;
import com.richieloco.coinsniper.service.binance.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService<AnnouncementResponse> {

    private final CoinSniperConfig.BinanceConfig apiConfig;
    private final WebClient webClient = WebClient.builder().build();

    public Mono<AnnouncementResponse> fetchAnnouncements(String baseUrl, String... params) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("type", params[0])
                        .queryParam("pageNo", params[1])
                        .queryParam("pageSize", params[2])
                        .build())
                .retrieve()
                .bodyToMono(AnnouncementResponse.class);
    }
}

