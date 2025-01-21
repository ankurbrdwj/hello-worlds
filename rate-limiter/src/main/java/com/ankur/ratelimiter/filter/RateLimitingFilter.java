package com.ankur.ratelimiter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws IOException, ServletException {

    String clientIp = request.getRemoteAddr();
    RateLimiter rateLimiter = rateLimiters.computeIfAbsent(clientIp, k -> new RateLimiter(5, 1, TimeUnit.MINUTES));

    if (rateLimiter.isRateLimited()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.getWriter().write("Rate limit exceeded. Try again later.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static class RateLimiter {
    private final int maxRequests;
    private final long timeWindowMillis;
    private long lastResetTime;
    private int requestCount;

    public RateLimiter(int maxRequests, long timeWindow, TimeUnit timeUnit) {
      this.maxRequests = maxRequests;
      this.timeWindowMillis = timeUnit.toMillis(timeWindow);
      this.lastResetTime = System.currentTimeMillis();
    }

    public synchronized boolean isRateLimited() {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastResetTime > timeWindowMillis) {
        lastResetTime = currentTime;
        requestCount = 0;
      }

      requestCount++;
      return requestCount > maxRequests;
    }
  }
}
