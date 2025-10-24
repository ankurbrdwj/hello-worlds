package com.ankur.webcrawler;

import com.ankur.webcrawler.service.Crawler;
import com.ankur.webcrawler.service.CrawlerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestPropertySource(properties = "crawler.seed-url=https://example.com")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class WebcrawlerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void shouldStartWithSeedUrl() {
        // Should NOT throw due to seedUrl being present
        assertDoesNotThrow(() -> {
            SpringApplication.run(WebcrawlerApplication.class);
        });
    }
}
