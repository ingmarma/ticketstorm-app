package com.ticketstorm.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("event-catalog", r -> r
                        .path("/api/v1/events/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8081"))
                .route("queue", r -> r
                        .path("/api/v1/queue/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8082"))
                .route("inventory", r -> r
                        .path("/api/v1/inventory/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8083"))
                .route("reservation", r -> r
                        .path("/api/v1/reservations/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8084"))
                .route("payment", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8085"))
                .route("notification", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8086"))
                .route("ai-discovery", r -> r
                        .path("/api/v1/ai/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("http://localhost:8087"))
                .build();
    }
}
