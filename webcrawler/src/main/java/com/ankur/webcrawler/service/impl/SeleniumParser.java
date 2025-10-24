package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.service.ParseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
@Slf4j
@Component("selenium")
public class SeleniumParser implements ParseStrategy {
    @Override
    public String parseWebPage(String url) throws IOException {
        // Use Selenium WebDriver to fetch rendered HTML
        System.setProperty("webdriver.chrome.driver", "/Users/ankurbrdwj/DriveC/java/hello-worlds/webcrawler/src/main/resources/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        String html;
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            driver.get(url);
            html = driver.getPageSource();
        } catch (TimeoutException e) {
            log.warn("⏱ Timeout loading {}: {}", url, e.getMessage());
            return ""; // skip or partial content
        } catch (Exception e) {
            log.error("❌ Failed fetching {}: {}", url, e.getMessage());
            throw new IOException(e);
        } finally {
            driver.quit();
        }
        return html;
    }
}
