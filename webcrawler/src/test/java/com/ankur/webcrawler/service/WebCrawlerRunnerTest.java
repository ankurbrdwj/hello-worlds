package com.ankur.webcrawler.service;

import com.ankur.webcrawler.command.CrawlerRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebCrawlerRunnerTest {

    @Mock
    private CrawlerFactory crawlerFactory;

    @Mock
    private Crawler crawler;

    @InjectMocks
    private CrawlerRunner crawlerRunner;

    @Test
    void testSeedUrlIsPassedToCrawler() {
        String seedUrl = "https://example.com";
        crawlerRunner.setSeedUrl(seedUrl);

        // This line is correct. It tells Mockito that when `createWebCrawler` is called
        // with the specific seedUrl and ANY integer, it should return your mock webCrawler.
        when(crawlerFactory.createMultiCrawler(eq(seedUrl), anyInt())).thenReturn(crawler);

        // Act
        crawlerRunner.run();

        // Verification Step 1: Check that the factory was called correctly.
        verify(crawlerFactory).createMultiCrawler(eq(seedUrl), anyInt());

        // Verification Step 2: Check that the crawl() method was called on the object the factory created.
        // THE FIX: The crawl() method in your WebCrawler takes no arguments.
        // The error occurred because you were trying to verify it with a matcher (any(Integer.class)),
        // but the method signature is empty. The verification must match the signature.
        verify(crawler).crawl(seedUrl);
    }
}
