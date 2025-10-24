package com.ankur.webcrawler.service;

import com.ankur.webcrawler.dto.CrawlResults;
import com.ankur.webcrawler.util.UniqueBlockingQueue;
import java.util.Set;

public interface Crawler {
    void crawl(String seedUrl);

    Set<String> getVisitedUrls();

    CrawlResults addLinksToQueue(String url);

    Set<String> extractUrls(String html, String url);

    boolean isSameDomain(String link);

    UniqueBlockingQueue<String> getFrontier();
}

