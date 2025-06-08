package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementCallController {

    private final AnnouncementCallingService announcementCallingService;
    private final CoinSniperConfig config;

    @GetMapping("/call")
    public Flux<CoinAnnouncementRecord> callBinance(
            @RequestParam Integer type,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize
    ) {
        var announcementCfg = config.getApi().getBinance().getAnnouncement();

        int resolvedType = type != null ? type : announcementCfg.getType();
        int resolvedPageNo = pageNo != null ? pageNo : announcementCfg.getPageNo();
        int resolvedPageSize = pageSize != null ? pageSize : announcementCfg.getPageSize();

        return announcementCallingService.callBinanceAnnouncements(resolvedType, resolvedPageNo, resolvedPageSize);
    }
}