package com.richieloco.coinsniper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@ConfigurationProperties(prefix = "coin-sniper.api")
@Data
@Validated
public class CoinSniperConfig {
    private BinanceConfig binance;
    private Map<String, ExchangeConfig> onExchange;

    @Data
    public static class BinanceConfig {

        @NotBlank
        private String baseUrl;

        @Min(1)
        private int type;

        @Min(1)
        private int pageNo;

        @Min(1)
        private int pageSize;
    }

    @Data
    public static class ExchangeConfig {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String apiKey;

        @NotBlank
        private String apiSecret;
    }
}
