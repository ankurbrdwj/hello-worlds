package com.ankur.candlesticks.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
@ConfigurationProperties(prefix = "partner")  // This prefix is from your config files (e.g., partner.enabled)
public class PartnerConfig {
  private boolean enabled;
  private String instrumentsUri;
  private String quotesUri;

}
