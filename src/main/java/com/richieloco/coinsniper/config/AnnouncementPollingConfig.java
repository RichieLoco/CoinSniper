package com.richieloco.coinsniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("coin-sniper.announcement-polling")
public class AnnouncementPollingConfig {
    private long intervalSeconds = 60;      // Default to 60 seconds
    private boolean enabled = true;         // enable/disable flag
}
