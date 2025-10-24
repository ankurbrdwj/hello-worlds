package com.ankur.webcrawler.service;

import com.ankur.webcrawler.dto.CrawlResults;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Callable;

@Slf4j
@Getter
public class CrawlWorker implements Callable<CrawlResults> {
    private final String url;
    private final ParseStrategy parser;
    private final CrawlReporterInterface reporter;
    private final Crawler crawler;

    public CrawlWorker(String url,
                       ParseStrategy parser,
                       CrawlReporterInterface reporter,
                       Crawler crawler) {
        this.url = url;
        this.parser = parser;
        this.reporter = reporter;
        this.crawler = crawler;
    }

    @Override
    public CrawlResults call() {
        return crawler.addLinksToQueue(url);
    }
}