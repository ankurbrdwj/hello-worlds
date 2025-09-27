package com.ankur.webcrawler.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
@Getter
@Setter
public class Frontier {
    private Deque<String> queue = new LinkedList<>();

    public void addUrl(String url) {
        queue.add(url);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Set<String> getAllUrls() {
        return new HashSet<>(queue);
    }
    public String pollUrl() {
        return queue.poll();
    }
}
