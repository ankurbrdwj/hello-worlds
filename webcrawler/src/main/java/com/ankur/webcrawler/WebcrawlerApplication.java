package com.ankur.webcrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WebcrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebcrawlerApplication.class, args);
    }

}
