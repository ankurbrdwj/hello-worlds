package com.ankur.webcrawler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.context.TestPropertySource;


@TestPropertySource(properties = "crawler.seed-url=https://example.com")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class WebcrawlerApplicationTests {

    @Test
    void contextLoads() {
    }
}
