package com.ankur.webcrawler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(args = "https://example.com")
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
