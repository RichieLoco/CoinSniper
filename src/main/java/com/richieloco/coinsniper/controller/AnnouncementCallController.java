package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/announcements")
@AllArgsConstructor
public class AnnouncementCallController {

    private final AnnouncementCallingService announcementCallingService;

    @GetMapping("/call")
    public Flux<CoinAnnouncementRecord> callBinance() {
        return announcementCallingService.callBinanceAnnouncements();
    }
}
