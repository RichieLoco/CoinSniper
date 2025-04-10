package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.AnnouncementResponse;
import com.richieloco.coinsniper.service.binance.impl.AnnouncementServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/binance")
@RequiredArgsConstructor
@Validated
public class BinanceAnnouncementController {

    private final AnnouncementServiceImpl announcementsService;
    private final CoinSniperConfig.BinanceConfig apiConfig;

    @GetMapping("/announcements")
    public Mono<AnnouncementResponse> getArticles(
            @RequestParam(defaultValue = "#{apiConfig.type}") @Min(1) String type,
            @RequestParam(defaultValue = "#{apiConfig.pageNo}") @Min(1) String pageNo,
            @RequestParam(defaultValue = "#{apiConfig.pageSize}") @Min(1) String pageSize) {
        return announcementsService.fetchAnnouncements(apiConfig.getBaseUrl(), type, pageNo, pageSize);
    }
}
