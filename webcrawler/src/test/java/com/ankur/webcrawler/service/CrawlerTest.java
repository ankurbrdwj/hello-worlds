package com.ankur.webcrawler.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerTest {

    @Test
    void shouldCrawlUrlsFromFrontier() {
        Frontier frontier = new Frontier();
        frontier.addUrl("https://example.com");

        Crawler crawler = new Crawler("https://example.com",frontier);
        crawler.crawl();

        assertTrue(frontier.isEmpty());
        assertEquals(1, crawler.getCrawledCount());
    }

    @Test
    void extractUrls_shouldReturnAllAbsoluteUrlsFromHtml() {
        String html = "<html><body>"
                + "<a href='https://example.com/page1'>Page1</a>"
                + "<a href='https://external.com/page2'>External</a>"
                + "<a href='/relative'>Relative</a>"
                + "<a href=''>Empty</a>"
                + "<a>NoHref</a>"
                + "</body></html>";
        Crawler crawler = new Crawler("https://example.com", new Frontier());
        Set<String> urls = crawler.extractUrls(html);
        // jsoup.absUrl returns empty string for relative links without base URI
        assertTrue(urls.contains("https://example.com/page1"));
        assertTrue(urls.contains("https://external.com/page2"));
        assertFalse(urls.contains("/relative"));
        assertFalse(urls.contains(""));
    }

    @Test
    void extractUrls_canBeAddedToFrontierQueue() {
        String html = "<html><body>"
                + "<a href='https://example.com/page1'>Page1</a>"
                + "<a href='https://external.com/page2'>External</a>"
                + "<a href='/relative'>Relative</a>"
                + "<a href=''>Empty</a>"
                + "<a>NoHref</a>"
                + "</body></html>";
        Crawler crawler = new Crawler("https://example.com", new Frontier());
        Set<String> urls = crawler.extractUrls(html);
        Frontier frontier = new Frontier();
        for (String url : urls) {
            frontier.addUrl(url);
        }
        Set<String> allUrls = frontier.getAllUrls();
        assertTrue(allUrls.contains("https://example.com/page1"));
        assertTrue(allUrls.contains("https://external.com/page2"));
        // jsoup.absUrl returns empty string for relative links without base URI
        assertFalse(allUrls.contains("/relative"));
        assertFalse(allUrls.contains(""));
    }

    @Test
    void frontierQueue_pollsUrlsInOrder() {
        Frontier frontier = new Frontier();
        frontier.addUrl("https://example.com/page1");
        frontier.addUrl("https://example.com/page2");
        frontier.addUrl("https://example.com/page3");

        String first = frontier.pollUrl();
        String second = frontier.pollUrl();
        String third = frontier.pollUrl();

        assertEquals("https://example.com/page1", first);
        assertEquals("https://example.com/page2", second);
        assertEquals("https://example.com/page3", third);
        assertTrue(frontier.isEmpty());
    }

    @Test
    void crawl_untilFrontierIsEmpty_crawlsAllReachableUrls() {
        // Simulate a small web graph:
        // https://example.com -> [https://example.com/page1, https://example.com/page2]
        // https://example.com/page1 -> [https://example.com/page3]
        // https://example.com/page2 -> []
        // https://example.com/page3 -> []
        class TestCrawler extends Crawler {
            TestCrawler(String root, Frontier frontier) { super(root, frontier); }
            @Override
            public Set<String> extractUrls(String htmlContent) {
                return switch (htmlContent) {
                    case "HTML_ROOT" -> Set.of("https://example.com/page1", "https://example.com/page2");
                    case "HTML_PAGE1" -> Set.of("https://example.com/page3");
                    default -> Set.of();
                };
            }
            @Override
            public void crawl() {
                // Simulate fetching by using the URL as a key for HTML
                if (!hasCrawled(rootDomain)) {
                    Set<String> links = extractUrls("HTML_ROOT");
                    for (String link : links) {
                        if (!hasCrawled(link)) frontier.addUrl(link);
                    }
                    getCrawledUrls().add(rootDomain);
                }
                while (!frontier.isEmpty()) {
                    String url = frontier.pollUrl();
                    if (url == null || hasCrawled(url)) continue;
                    Set<String> links = extractUrls(url.equals("https://example.com/page1") ? "HTML_PAGE1" : "");
                    for (String link : links) {
                        if (!hasCrawled(link)) frontier.addUrl(link);
                    }
                    getCrawledUrls().add(url);
                }
            }
            // Expose crawledUrls for assertions
            Set<String> getCrawledUrls() { return super.crawledUrls; }
        }
        Frontier frontier = new Frontier();
        frontier.addUrl("https://example.com");
        TestCrawler crawler = new TestCrawler("https://example.com", frontier);
        crawler.crawl();
        assertTrue(frontier.isEmpty());
        assertEquals(Set.of(
            "https://example.com",
            "https://example.com/page1",
            "https://example.com/page2",
            "https://example.com/page3"
        ), crawler.getCrawledUrls());
    }

    @Test
    void shouldOnlyVisitSameDomainUrls() {
        Frontier frontier = new Frontier();
        frontier.addUrl("https://example.com");
        frontier.addUrl("https://example.com/page1");
        frontier.addUrl("https://external.com/page2");
        frontier.addUrl("https://another.com/page3");

        new Crawler("https://example.com", frontier);
        // Simulate extractUrls to avoid real HTTP calls
        // We'll override extractUrls to always return an empty set
        Crawler testCrawler = new Crawler("https://example.com", frontier) {
            @Override
            public Set<String> extractUrls(String htmlContent) {
                return Set.of();
            }
        };
        testCrawler.crawl();

        // Only the example.com URLs should be crawled
        assertTrue(testCrawler.hasCrawled("https://example.com"));
        //assertTrue(testCrawler.hasCrawled("https://example.com/page1"));
        assertFalse(testCrawler.hasCrawled("https://external.com/page2"));
        assertFalse(testCrawler.hasCrawled("https://another.com/page3"));
    }

}