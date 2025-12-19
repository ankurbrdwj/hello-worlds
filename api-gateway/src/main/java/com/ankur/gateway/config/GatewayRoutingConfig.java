package com.ankur.gateway.config;

import com.ankur.gateway.ratelimiter.RateLimiterFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * Gateway Routing Configuration with Rate Limiting
 *
 * FLOW:
 * 1. Request arrives at Gateway (port 8080)
 * 2. Rate Limiter checks if request is allowed
 * 3. If allowed → route to downstream service
 * 4. If denied → return 429 Too Many Requests
 *
 * Rate Limiter Types (switch via property):
 * - gateway.ratelimiter.type=resilience4j  (in-memory, default)
 * - gateway.ratelimiter.type=redis         (distributed)
 */
@Configuration
public class GatewayRoutingConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingConfig.class);

    private final RateLimiterFilter rateLimiterFilter;

    // Define your microservice URLs here
    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static final String ORDER_SERVICE_URL = "http://localhost:8082";

    public GatewayRoutingConfig(RateLimiterFilter rateLimiterFilter) {
        this.rateLimiterFilter = rateLimiterFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        log.info("=== Initializing Gateway Routes ===");
        log.info("Rate Limiter Type: {}", rateLimiterFilter.getRateLimiterType());
        log.info("Route [user-service]    : /api/users/**    -> {}", USER_SERVICE_URL);
        log.info("Route [order-service]   : /api/orders/**   -> {}", ORDER_SERVICE_URL);

        return
            // Route 1: User Service - GET /api/users/**
            route("user-service")
                .GET("/api/users/**", http())
                .before(rateLimiterFilter::filter)  // Rate limit first!
                .before(uri(USER_SERVICE_URL))
                .before(request -> logRouteMatched("user-service", request, USER_SERVICE_URL))
                .after((request, response) -> logRouteCompleted("user-service", request, response))
                .onError(RateLimiterFilter.RateLimitExceededException.class,
                        (ex, request) -> rateLimiterFilter.handleRateLimitExceeded((RateLimiterFilter.RateLimitExceededException) ex, request))
                .build()

            // Route 2: Order Service - GET /api/orders/**
            .and(route("order-service")
                .GET("/api/orders/**", http())
                .before(rateLimiterFilter::filter)  // Rate limit first!
                .before(uri(ORDER_SERVICE_URL))
                .before(request -> logRouteMatched("order-service", request, ORDER_SERVICE_URL))
                .after((request, response) -> logRouteCompleted("order-service", request, response))
                .onError(RateLimiterFilter.RateLimitExceededException.class,
                        (ex, request) -> rateLimiterFilter.handleRateLimitExceeded((RateLimiterFilter.RateLimitExceededException) ex, request))
                .build())

            // Route 3: User Service - POST /api/users/**
            .and(route("user-service-post")
                .POST("/api/users/**", http())
                .before(rateLimiterFilter::filter)  // Rate limit first!
                .before(uri(USER_SERVICE_URL))
                .before(request -> logRouteMatched("user-service-post", request, USER_SERVICE_URL))
                .after((request, response) -> logRouteCompleted("user-service-post", request, response))
                .onError(RateLimiterFilter.RateLimitExceededException.class,
                        (ex, request) -> rateLimiterFilter.handleRateLimitExceeded((RateLimiterFilter.RateLimitExceededException) ex, request))
                .build())

            // Route 4: Order Service - POST /api/orders/**
            .and(route("order-service-post")
                .POST("/api/orders/**", http())
                .before(rateLimiterFilter::filter)  // Rate limit first!
                .before(uri(ORDER_SERVICE_URL))
                .before(request -> logRouteMatched("order-service-post", request, ORDER_SERVICE_URL))
                .after((request, response) -> logRouteCompleted("order-service-post", request, response))
                .onError(RateLimiterFilter.RateLimitExceededException.class,
                        (ex, request) -> rateLimiterFilter.handleRateLimitExceeded((RateLimiterFilter.RateLimitExceededException) ex, request))
                .build())

            // Fallback route
            .and(createFallbackRoute());
    }

    private ServerRequest logRouteMatched(String routeId, ServerRequest request, String targetUri) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║ ROUTE MATCHED                                                ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Route ID    : {}", padRight(routeId, 48) + "║");
        log.info("║ Method      : {}", padRight(request.method().name(), 48) + "║");
        log.info("║ Path        : {}", padRight(request.path(), 48) + "║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ FORWARDING TO:                                               ║");
        log.info("║ Target URI  : {}", padRight(targetUri, 48) + "║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        return request;
    }

    private ServerResponse logRouteCompleted(String routeId, ServerRequest request, ServerResponse response) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║ ROUTE COMPLETED                                              ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Route ID    : {}", padRight(routeId, 48) + "║");
        log.info("║ Status      : {}", padRight(String.valueOf(response.statusCode().value()), 48) + "║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        return response;
    }

    private RouterFunction<ServerResponse> createFallbackRoute() {
        return route("fallback")
                .GET("/**", request -> {
                    log.warn("No route matched for: {} {}", request.method(), request.path());
                    return ServerResponse.status(404)
                            .body("No route found for: " + request.method() + " " + request.path());
                })
                .build();
    }

    private String padRight(String s, int length) {
        if (s == null) s = "null";
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }
}