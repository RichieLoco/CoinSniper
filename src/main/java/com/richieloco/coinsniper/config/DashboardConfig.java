package com.richieloco.coinsniper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dashboard")
public class DashboardConfig {
    private boolean enabled;
    private int maxResults;
    private String defaultExchange;
}