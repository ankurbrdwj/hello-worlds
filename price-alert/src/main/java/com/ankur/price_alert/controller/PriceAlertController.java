package com.ankur.price_alert.controller;

import com.ankur.price_alert.model.Alert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/alerts")
public class PriceAlertController {

    private final List<Alert> alerts;

    public PriceAlertController() {
        // Preload 1M alerts for testing
        alerts = new ArrayList<>();
        for (int i = 1; i <= 1_000_000; i++) {
            alerts.add(new Alert(i, true));
        }
        alerts.sort(Comparator.comparingDouble(Alert::getTargetPrice));
    }

    // 1. Sorted List + Binary Search
    @GetMapping("/binary")
    public int evaluateBinary(@RequestParam double price) {
        int idx = Collections.binarySearch(alerts, new Alert(price, true),
                Comparator.comparingDouble(Alert::getTargetPrice));
        if (idx < 0) idx = -(idx + 1);
        return idx; // number of triggered alerts
    }

    // 2. Min-Heap approach
    @GetMapping("/heap")
    public int evaluateHeap(@RequestParam double price) {
        PriorityQueue<Alert> heap = new PriorityQueue<>(alerts);
        int count = 0;
        while (!heap.isEmpty() && heap.peek().getTargetPrice() <= price) {
            heap.poll();
            count++;
        }
        return count;
    }

    // 3. Bucketization approach
    @GetMapping("/bucket")
    public int evaluateBucket(@RequestParam double price) {
        int bucketSize = 100;
        Map<Integer, List<Alert>> buckets = new HashMap<>();
        for (Alert a : alerts) {
            int bucketId = (int) (a.getTargetPrice() / bucketSize);
            buckets.computeIfAbsent(bucketId, k -> new ArrayList<>()).add(a);
        }
        int bucketId = (int) (price / bucketSize);
        return (int) buckets.getOrDefault(bucketId, List.of())
                .stream().filter(a -> a.getTargetPrice() <= price).count();
    }
}
