package com.ankur.webcrawler.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class CrawlerRunner implements CommandLineRunner {
    @Value("${crawler.seed-url:}")
    private String seedUrl;

    @Override
    public void run(String... args) {
        if (seedUrl.isBlank()) {
            System.err.println("""
                ❌ Missing required option: --crawler.seed-url=<url>
                Example:
                  java -jar webcrawler.jar --crawler.seed-url=https://monzo.com --crawler.max-depth=2
                """);
            throw new IllegalArgumentException("Usage: --crawler.seed-url=...");
        }
        System.out.println("✅ Starting crawl from: " + seedUrl);
        new Crawler(seedUrl, null);

    }
}