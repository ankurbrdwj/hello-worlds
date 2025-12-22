package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.dto.CrawlResults;
import com.ankur.webcrawler.service.CrawlReporterInterface;
import com.ankur.webcrawler.service.CrawlWorker;
import com.ankur.webcrawler.service.Crawler;
import com.ankur.webcrawler.service.ParseStrategy;
import com.ankur.webcrawler.util.UniqueBlockingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class MultithreadedCrawler implements Crawler {
    static String interruptMessage ="Interrupted!";
    private final String seedUrl;
    private final int maxDepth;
    private final CrawlReporterInterface reporter;
    private final ParseStrategy parseStrategy;

    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final UniqueBlockingQueue<String> frontier = new UniqueBlockingQueue<>();
    private final ExecutorService pool;

    public MultithreadedCrawler(@Value("${seed-url}") String seedUrl,
                                @Value("${max-depth}") int maxDepth,
                                @Value("${pool-size:1}") int poolSize,
                                CrawlReporterInterface reporter,
                                ParseStrategy parseStrategy) {
        this.seedUrl = seedUrl;
        this.maxDepth = maxDepth;
        this.reporter = reporter;
        this.parseStrategy = parseStrategy;
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void crawl(String seedUrl) {
        long start = System.nanoTime();

        // can we add the seed URl to the queue and
        // then refactor the read from queue to separate method
        CrawlResults crawlResults;
        if (!visitedUrls.contains(seedUrl)) {
            visitedUrls.add(seedUrl);
            crawlResults = addLinksToQueue(seedUrl);
            reporter.report(seedUrl, crawlResults,maxDepth);
        }

        while (frontier.isEmpty()) {
            String url = null;
            try {
                url = frontier.take();
            } catch (InterruptedException e) {
                log.warn(interruptMessage, e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            }
            if (url == null || !visitedUrls.add(url)) continue;

            Future<CrawlResults> future = pool.submit(
                    new CrawlWorker(url, parseStrategy, reporter, this)
            );

            try {
                CrawlResults result = future.get(); // sequential wait; pool-size=1 means serial
                log.info("Crawled {} ({} new links)", url, result.foundLinks().size());
            } catch (InterruptedException e) {
                log.warn("INTERRUPT_MESSAGE", e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.warn("ExecutionException!", e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();            }
        }

        pool.shutdown();
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        log.info(" Crawl complete in {} seconds ({} URLs)", seconds, visitedUrls.size());
    }

    @Override
    public Set<String>  getVisitedUrls() {
        return this.visitedUrls;
    }

    @Override
    public UniqueBlockingQueue<String> getFrontier() {
        return this.frontier;
    }

    @Override
    /*
    the method name is addLinkstoQueue but iot parsing and fetching the URls
    lets move fetch and url parsing to separate class
     */
    public CrawlResults addLinksToQueue(String url) {
        String html = "";
        try {
            html = fetchHtml(url);
        } catch (IOException e) {
            log.error("Failed to fetch or process {}: {}", url, e.getMessage());
        }
        log.debug("Fetched HTML for {}: {}", url, html.substring(0, Math.min(200, html.length())));
        // Move it to parsing classes
        Set<String> links = extractUrls(html, url);
        log.info("Extracted {} links from {}", links.size(), url);
        log.info("links already crawled : {}", visitedUrls.size());
        links = links.stream()
                //.limit(maxDepth)
                .filter(link -> isSameDomain(link) && !visitedUrls.contains(link))
                .collect(Collectors.toSet());

        links.forEach(link -> {
            try {
                frontier.put(link);
            } catch (InterruptedException e) {
                log.warn(interruptMessage, e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();            }
        });
        log.info("Current queue size {}", frontier.size());

        return new CrawlResults(url, links);
    }

    private String fetchHtml(String url) throws IOException {
        return parseStrategy.parseWebPage(url);
    }

    @Override
    public boolean isSameDomain(String url) {
        try {
            var root = new java.net.URI(seedUrl);
            var test = new java.net.URI(url);
            return Objects.equals(root.getHost(), test.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Set<String> extractUrls(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        return doc.select("a[href]").stream()
                .map(link -> link.absUrl("href"))
                .filter(l -> !l.isBlank())
                .collect(Collectors.toSet());
    }
}
