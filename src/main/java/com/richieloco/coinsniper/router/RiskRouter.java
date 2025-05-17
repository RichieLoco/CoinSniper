package com.richieloco.coinsniper.router;

import com.richieloco.coinsniper.handler.ExchangeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class RiskRouter {

    @Bean
    public RouterFunction<?> routes(ExchangeHandler handler) {
        return route(GET("/api/risk/exchange"), handler::exchangeRisk)
                .andRoute(GET("/api/risk/coin"), handler::coinRisk);
    }
}
