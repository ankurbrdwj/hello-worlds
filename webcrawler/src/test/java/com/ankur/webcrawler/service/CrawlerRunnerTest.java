package com.ankur.webcrawler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerRunnerTest {

    private CrawlerRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CrawlerRunner();
    }

    @Test
    void shouldStartCrawlWhenSeedUrlProvided() {
        runner.setSeedUrl("https://monzo.com");
        assertDoesNotThrow(() -> runner.run());
    }

}