package com.ankur.ratelimiter.configuration;

import com.ankur.ratelimiter.filter.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitingConfiguration {
  @Bean
  public FilterRegistrationBean<RateLimitingFilter> rateLimitingConfig() {
    FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RateLimitingFilter());
    registrationBean.addUrlPatterns("/api/*");
    registrationBean.setOrder(1); // Set the order of the filter
    return registrationBean;
  }

}
