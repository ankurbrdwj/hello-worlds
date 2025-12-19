package com.ankur.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Request counters per service
    private final Map<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> successCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> rateLimitedCounters = new ConcurrentHashMap<>();

    // Total counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalRateLimited = new AtomicLong(0);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();
        String service = extractService(request.getRequestURI());

        // Increment request counter
        incrementCounter(requestCounters, service);
        long reqCount = totalRequests.incrementAndGet();

        logRequestEntry(request, service, reqCount);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestCompletion(request, response, service, duration);
            MDC.remove("requestId");
        }
    }

    private void logRequestEntry(HttpServletRequest request, String service, long totalCount) {
        log.info(">>> [{}] {} {} | Total: {} | Service requests: {}",
                service,
                request.getMethod(),
                request.getRequestURI(),
                totalCount,
                getCount(requestCounters, service));
    }

    private void logRequestCompletion(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String service,
                                      long duration) {
        int status = response.getStatus();

        if (status == 429) {
            // Rate limited
            incrementCounter(rateLimitedCounters, service);
            long rateLimited = totalRateLimited.incrementAndGet();

            log.warn("<<< [{}] {} {} | Status: {} RATE_LIMITED | Duration: {}ms | Rate limited: {}/{}",
                    service,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    rateLimited,
                    totalRequests.get());
        } else if (status < 400) {
            // Success
            incrementCounter(successCounters, service);

            log.info("<<< [{}] {} {} | Status: {} OK | Duration: {}ms | Success rate: {}/{}",
                    service,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    getCount(successCounters, service),
                    getCount(requestCounters, service));
        } else {
            // Error
            log.error("<<< [{}] {} {} | Status: {} ERROR | Duration: {}ms",
                    service,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration);
        }
    }

    private String extractService(String uri) {
        if (uri == null) return "unknown";
        if (uri.startsWith("/api/users")) return "user-service";
        if (uri.startsWith("/api/orders")) return "order-service";
        if (uri.startsWith("/api/ratelimit")) return "rate-limiter";
        return "gateway";
    }

    private void incrementCounter(Map<String, AtomicLong> counters, String service) {
        counters.computeIfAbsent(service, k -> new AtomicLong(0)).incrementAndGet();
    }

    private long getCount(Map<String, AtomicLong> counters, String service) {
        AtomicLong counter = counters.get(service);
        return counter != null ? counter.get() : 0;
    }
}