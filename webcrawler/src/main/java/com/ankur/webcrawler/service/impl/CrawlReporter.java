package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.dto.CrawlResults;
import com.ankur.webcrawler.service.CrawlReporterInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Set;

@Component
@Slf4j
public class CrawlReporter implements CrawlReporterInterface {
    public void printUrls(Set<String> urls) {
        urls.forEach(log::info);
    }

    @Override
    public void report(String seedUrl, CrawlResults crawlResults, int depth) {
        log.info("Base url Found : {}", seedUrl);
        log.info("Found links  : {}",crawlResults.foundLinks().size());
        Iterator<String> iterator = crawlResults.foundLinks().iterator();
        for (int i = 0; i < depth; i++) {
            if (iterator.hasNext()) {
                log.info(iterator.next());
            }
        }
    }
}

