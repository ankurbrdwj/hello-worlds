package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.dto.CrawlResults;
import com.ankur.webcrawler.service.CrawlReporterInterface;
import com.ankur.webcrawler.service.Crawler;
import com.ankur.webcrawler.service.ParseStrategy;
import com.ankur.webcrawler.util.UniqueBlockingQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ankur.webcrawler.service.impl.MultithreadedCrawler.interruptMessage;

@Component
@Slf4j
@Getter
@Setter
public class WebCrawler implements Crawler {

    final String seedUrl;
    final int maxDepth;
    final CrawlReporterInterface reporter;
    final ParseStrategy parseStrategy;

    public WebCrawler(@Value("${seed-url}") String seedUrl, @Value("${max-depth}") int maxDepth, CrawlReporterInterface reporter
            ,ParseStrategy parseStrategy) {
        this.seedUrl = seedUrl;
        this.maxDepth = maxDepth;
        this.reporter = reporter;
        this.parseStrategy = parseStrategy;
    }

    private UniqueBlockingQueue<String> queue = new UniqueBlockingQueue<>();
    final Set<String> crawledUrls = new HashSet<>();

    @Override
    public void crawl(String seedUrl) {
        log.info("Processing the root URL: {}", seedUrl);
        CrawlResults crawlResults;
        if (!crawledUrls.contains(seedUrl)) {
            crawledUrls.add(seedUrl);
            crawlResults = addLinksToQueue(seedUrl);
            reporter.report(seedUrl, crawlResults, maxDepth);
        }

        while (queue.isEmpty()) {
            String url = null;
            try {
                url = queue.take();
            } catch (InterruptedException e) {
                log.warn(interruptMessage, e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            }
            if (url == null || crawledUrls.contains(url)) continue;
            crawledUrls.add(url);
            crawlResults = addLinksToQueue(url);
            reporter.report(url, crawlResults, maxDepth);
        }
    }
    @Override
    public CrawlResults addLinksToQueue(String url) {
        String html = "";
        try {
            html = fetchHtml(url);
        } catch (IOException e) {
            log.error("Failed to fetch or process {}: {}", url, e.getMessage());
        }
        log.debug("Fetched HTML for {}: {}", url, html.substring(0, Math.min(200, html.length())));
        Set<String> links = extractUrls(html, url);
        log.info("Extracted {} links from {}", links.size(), url);
        log.info("links already crawled : {}", crawledUrls.size());
        links=links.stream()
                //.limit(maxDepth)
                .filter(link -> isSameDomain(link) && !crawledUrls.contains(link))
                .collect(Collectors.toSet());

        links.forEach(link -> {
            try {
                queue.put(link);
            } catch (InterruptedException e) {
                log.warn(interruptMessage, e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            }
        });
        log.info("Current queue size {}", queue.size());

        return new CrawlResults(url, links);
    }

    protected String fetchHtml(String url) throws IOException {
        return parseStrategy.parseWebPage(url);
    }

    public boolean isSameDomain(String url) {
        // Improved domain check
        try {
            java.net.URI rootUri = new java.net.URI(seedUrl);
            java.net.URI testUri = new java.net.URI(url);
            return rootUri.getHost() != null && rootUri.getHost().equalsIgnoreCase(testUri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Set<String>  getVisitedUrls() {
        return this.crawledUrls;
    }

    @Override
    public UniqueBlockingQueue<String> getFrontier() {
        return this.queue;
    }

    public Set<String> extractUrls(String htmlContent, String baseUrl) {
        Document doc = Jsoup.parse(htmlContent, baseUrl);
        Elements linkElements = doc.select("a[href]");
        log.info("Found {} <a> tags on {}", linkElements.size(), baseUrl);
        Set<String> hyperlinks = linkElements.stream()
                .map(link -> link.absUrl("href"))
                .filter(absHref -> !absHref.isEmpty())
                .filter(absHref -> !queue.contains(absHref))  // Filter out URLs already in queue
                .collect(Collectors.toSet());
        log.info("Returning {} unique hyperlinks from {}", hyperlinks.size(), baseUrl);
        return hyperlinks;
    }
}
