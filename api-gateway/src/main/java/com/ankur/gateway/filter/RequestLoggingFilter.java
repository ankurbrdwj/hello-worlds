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
import java.util.Collections;
import java.util.UUID;

/**
 * Servlet Filter that logs every request passing through the Gateway.
 *
 * FILTER EXECUTION ORDER IN GATEWAY:
 *
 * 1. [RequestLoggingFilter] - This filter (Servlet level)
 * 2. [DispatcherServlet] - Spring MVC dispatcher
 * 3. [RouterFunction] - Gateway route matching
 * 4. [Before Filters] - Route-specific before filters
 * 5. [HTTP Forward] - Actual call to downstream service
 * 6. [After Filters] - Route-specific after filters
 * 7. [Response] - Back through filter chain
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();

        // Log request entry
        logRequestEntry(request, requestId);

        try {
            // Continue filter chain - this is where routing happens
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log request completion
            logRequestCompletion(request, response, requestId, duration);

            MDC.remove("requestId");
        }
    }

    private void logRequestEntry(HttpServletRequest request, String requestId) {
        log.info("");
        log.info("┌─────────────────────────────────────────────────────────────────┐");
        log.info("│ >>> REQUEST ENTERING GATEWAY                                    │");
        log.info("├─────────────────────────────────────────────────────────────────┤");
        log.info("│ Request ID   : {}                                         │", requestId);
        log.info("│ Timestamp    : {}                              │", java.time.Instant.now());
        log.info("├─────────────────────────────────────────────────────────────────┤");
        log.info("│ Method       : {}", padRight(request.getMethod(), 51) + "│");
        log.info("│ URI          : {}", padRight(request.getRequestURI(), 51) + "│");
        log.info("│ Query String : {}", padRight(request.getQueryString(), 51) + "│");
        log.info("│ Remote Addr  : {}", padRight(request.getRemoteAddr(), 51) + "│");
        log.info("│ Content-Type : {}", padRight(request.getContentType(), 51) + "│");
        log.info("├─────────────────────────────────────────────────────────────────┤");
        log.info("│ GATEWAY ROUTING FLOW:                                           │");
        log.info("│   Step 1: Request received at Gateway port                      │");
        log.info("│   Step 2: Matching predicates against routes...                 │");
        log.info("└─────────────────────────────────────────────────────────────────┘");

        // Log headers at DEBUG level
        if (log.isDebugEnabled()) {
            log.debug("Request Headers for [{}]:", requestId);
            Collections.list(request.getHeaderNames()).forEach(headerName ->
                    log.debug("  {} = {}", headerName, request.getHeader(headerName))
            );
        }
    }

    private void logRequestCompletion(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String requestId,
                                      long duration) {
        String statusEmoji = response.getStatus() < 400 ? "OK" : "ERR";

        log.info("");
        log.info("┌─────────────────────────────────────────────────────────────────┐");
        log.info("│ <<< RESPONSE LEAVING GATEWAY                                    │");
        log.info("├─────────────────────────────────────────────────────────────────┤");
        log.info("│ Request ID   : {}                                         │", requestId);
        log.info("│ Status       : {} [{}]", padRight(String.valueOf(response.getStatus()), 45) + statusEmoji, "│");
        log.info("│ Duration     : {} ms", padRight(String.valueOf(duration), 48) + "│");
        log.info("│ Path         : {}", padRight(request.getRequestURI(), 51) + "│");
        log.info("├─────────────────────────────────────────────────────────────────┤");
        log.info("│ ROUTING COMPLETED:                                              │");
        log.info("│   Step 5: Response received from downstream                     │");
        log.info("│   Step 6: Returning response to client                          │");
        log.info("└─────────────────────────────────────────────────────────────────┘");
        log.info("");
    }

    private String padRight(String s, int length) {
        if (s == null) s = "null";
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }
}