package com.richieloco.coinsniper.router;

import com.richieloco.coinsniper.handler.AnnouncementHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class AnnouncementRouter {

    @Bean
    public RouterFunction<ServerResponse> routeAnnouncements(AnnouncementHandler handler) {
        return RouterFunctions
                .nest(path("/announcements"),
                        RouterFunctions.route(GET("/fetch"), handler::getAnnouncements));
    }
}
