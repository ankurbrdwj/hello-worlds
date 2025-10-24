package com.ankur.webcrawler.service;

import com.ankur.webcrawler.dto.CrawlResults;

import java.util.*;

public class TestCrawlReporter implements CrawlReporterInterface {
    public final List<String> visitedUrls = new ArrayList<>();
    public final Map<String, Set<String>> linksPerUrl = new HashMap<>();
    public final Map<String, Integer> depthPerUrl = new HashMap<>();

    @Override
    public void report(String seedUrl, CrawlResults crawlResults, int depth) {
        visitedUrls.add(seedUrl);
        linksPerUrl.put(seedUrl, new HashSet<>(crawlResults.foundLinks()));
        depthPerUrl.put(seedUrl, depth);
    }
}
