package com.ankur.webcrawler.dto;

import java.util.Set;

/**
 * An immutable record to hold the results of crawling a single URL.
 *
 * @param crawledUrl The URL that was processed by the task.
 * @param foundLinks A set of all unique, absolute URLs found on that page.
 */
public record CrawlResults(String crawledUrl, Set<String> foundLinks) {
}
