package com.ankur.webcrawler.service;

import com.ankur.webcrawler.dto.CrawlResults;

public interface CrawlReporterInterface {
    void report(String seedUrl, CrawlResults crawlResults, int depth);
}