package com.ankur.webcrawler.service;

import com.ankur.webcrawler.service.impl.MultithreadedCrawler;
import com.ankur.webcrawler.service.impl.WebCrawler;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultCrawlerFactory implements CrawlerFactory {
    private final CrawlReporterInterface reporter;
    private final Map<String, ParseStrategy> strategies;
    private final Environment env;

    public DefaultCrawlerFactory(Map<String, ParseStrategy> strategies, Environment env, CrawlReporterInterface reporter) {
        this.strategies = strategies;
        this.env = env;
        this.reporter = reporter;
    }

    @Override
    public WebCrawler createWebCrawler(String seedUrl, int maxDepth) {
        String chosen = env.getProperty("crawler.parse-strategy", "jsoup");
        ParseStrategy strategy = strategies.get(chosen);

        if (strategy == null) {
            throw new IllegalArgumentException("Unknown parse strategy: " + chosen);
        }
        return new WebCrawler(seedUrl, maxDepth, reporter, strategy
        );
    }

    @Override
    public Crawler createMultiCrawler(String seedUrl, int maxDepth) {
        String chosen = env.getProperty("crawler.parse-strategy", "jsoup");
        ParseStrategy strategy = strategies.get(chosen);

        if (strategy == null) {
            throw new IllegalArgumentException("Unknown parse strategy: " + chosen);
        }
        return new MultithreadedCrawler(seedUrl, maxDepth,1, reporter, strategy);
    }
}
