package com.ankur.webcrawler.service;

import com.ankur.webcrawler.service.impl.WebCrawler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebCrawlerTest {

    @Mock
    private CrawlReporterInterface mockReporter;
    @Mock
    private ParseStrategy parseStrategy;

    @Test
    void extractUrls_shouldNotReturn_urlAlreadyInQueue() throws InterruptedException {
        // Arrange
        String seedUrl = "https://example.com";
        WebCrawler crawler = new WebCrawler(seedUrl, 5, mockReporter, parseStrategy);

        String urlAlreadyInQueue = "https://example.com/page1";
        String newUrl = "https://example.com/page2";

        // Add a URL to the crawler's internal queue before extraction
        crawler.getQueue().put(urlAlreadyInQueue);

        String htmlContent = "<html><body>"
                + "<a href='" + urlAlreadyInQueue + "'>Page 1</a>"
                + "<a href='" + newUrl + "'>Page 2</a>"
                + "</body></html>";

        // Act
        Set<String> extractedUrls = crawler.extractUrls(htmlContent, seedUrl);

        // Assert
        assertFalse(extractedUrls.contains(urlAlreadyInQueue), "The URL that was already in the queue should NOT be in the result set.");
        assertTrue(extractedUrls.contains(newUrl), "The new URL SHOULD be in the result set.");
        assertEquals(1, extractedUrls.size(), "The result set should contain exactly one URL.");
    }
}
