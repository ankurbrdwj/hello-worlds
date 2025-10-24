package com.ankur.webcrawler.config;

import com.ankur.webcrawler.service.CrawlReporterInterface;
import com.ankur.webcrawler.service.Crawler;
import com.ankur.webcrawler.service.ParseStrategy;
import com.ankur.webcrawler.service.impl.WebCrawler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class CrawlerConfig {
    @Value("${seed-url}")
    String seedUrl;
    @Value("${max-depth}")
    int maxDepth;
    private final Environment env;
    private final Map<String, ParseStrategy> strategies;
    private final CrawlReporterInterface reporter;

    public CrawlerConfig(Environment env,
                         Map<String, ParseStrategy> strategies,
                         CrawlReporterInterface reporter) {
        this.env = env;
        this.strategies = strategies;
        this.reporter = reporter;
    }

    @Bean
    public Crawler webCrawler() {
        String selected = env.getProperty("crawler.parse-strategy", "jsoup");
        ParseStrategy strategy = strategies.get(selected);

        if (strategy == null) {
            throw new IllegalArgumentException("Unknown parse strategy: " + selected);
        }
        return new WebCrawler(seedUrl, maxDepth, reporter, strategy);
    }
}
