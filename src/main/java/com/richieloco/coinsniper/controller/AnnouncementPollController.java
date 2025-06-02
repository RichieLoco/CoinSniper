package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.service.AnnouncementPollingScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements/poll")
@RequiredArgsConstructor
public class AnnouncementPollController {

    private final AnnouncementPollingScheduler scheduler;

    @PostMapping("/start")
    public String start() {
        scheduler.startPolling();
        return "Polling started.";
    }

    @PostMapping("/stop")
    public String stop() {
        scheduler.stopPolling();
        return "Polling stopped.";
    }

    @GetMapping("/status")
    public String status() {
        return scheduler.isPollingActive() ? "Polling is active." : "Polling is stopped.";
    }
}
