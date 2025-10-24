package com.ankur.webcrawler.service;


public interface CrawlerFactory {
    Crawler createWebCrawler(String seedUrl, int maxDepth);
    Crawler createMultiCrawler(String seedUrl, int maxDepth);
}
