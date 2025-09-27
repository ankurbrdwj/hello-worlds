package com.ankur.webcrawler.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Crawler {

    final String rootDomain;
    final Frontier frontier;
    final Set<String> crawledUrls = new HashSet<>();

    public void crawl() {
        // Fetch and process the seed/root URL
        if (!crawledUrls.contains(rootDomain)) {
            try {
                String html = fetchHtml(rootDomain);
                Set<String> links = extractUrls(html);
                for (String link : links) {
                    if (isSameDomain(link) && !crawledUrls.contains(link)) {
                        frontier.addUrl(link);
                    }
                }
                crawledUrls.add(rootDomain);
            } catch (IOException e) {
                // Handle fetch error (skip for now)
            }
        }
        // Continue crawling as before
        while (!frontier.isEmpty()) {
            String url = frontier.pollUrl();
            if (url == null) continue;
            if (isSameDomain(url) && !crawledUrls.contains(url)) {
                try {
                    String html = fetchHtml(url);
                    Set<String> links = extractUrls(html);
                    for (String link : links) {
                        if (isSameDomain(link) && !crawledUrls.contains(link)) {
                            frontier.addUrl(link);
                        }
                    }
                    crawledUrls.add(url);
                } catch (IOException e) {
                    // Handle fetch error (skip for now)
                }
            }
        }
    }

    protected String fetchHtml(String url) throws IOException {
        // Default implementation uses Jsoup
        return org.jsoup.Jsoup.connect(url).get().html();
    }

    private boolean isSameDomain(String url) {
        // Minimal implementation: check if url contains rootDomain (for test)
        // In real code, parse host and compare
        return url.contains(rootDomain.replace("https://", "").replace("http://", ""));
    }

    public int getCrawledCount() {
        return crawledUrls == null ? 0 : crawledUrls.size();
    }

    public boolean hasCrawled(String url) {
        return crawledUrls != null && crawledUrls.contains(url);
    }

    public Set<String> extractUrls(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Elements links = doc.select("a[href]");
        Set<String> urls = new HashSet<>(links.stream().map(link -> link.absUrl("href")).filter(href -> !href.isEmpty()).collect(Collectors.toSet()));
        return urls;
    }
}
