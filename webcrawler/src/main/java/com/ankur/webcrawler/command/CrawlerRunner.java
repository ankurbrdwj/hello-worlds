package com.ankur.webcrawler.command;

import com.ankur.webcrawler.service.Crawler;
import com.ankur.webcrawler.service.CrawlerFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class CrawlerRunner implements CommandLineRunner {
    @Value("${seed-url:}")
    private String seedUrl;

    @Value("${max-depth:5}")
    private int maxDepth;

    private final CrawlerFactory crawlerFactory;

    @Override
    public void run(String... args) {
        if (seedUrl.isBlank()) {
            log.error("Missing required option: --crawler.seed-url=<url>  " +
                    "Example: java -jar webcrawler.jar --crawler.seed-url=https://monzo.com --max-depth=2 ");
            throw new IllegalArgumentException("Usage: --crawler.seed-url=...");
        }
        log.info("Starting crawl from: {}", seedUrl);
        crawlerFactory
                .createMultiCrawler(seedUrl, maxDepth)
                .crawl(seedUrl);
    }
}