package com.ankur.price_alert.service;

import com.ankur.price_alert.model.Alert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriceBinarySearchTest {
    @Test
    void testSortedListBinarySearch() {
        List<Alert> alerts = Arrays.asList(
                new Alert(100, true),  // trigger when price >= 100
                new Alert(150, true),
                new Alert(200, true)
        );
        alerts.sort(Comparator.comparingDouble(Alert::getTargetPrice));

        double currentPrice = 160;

        // Binary search to find boundary
        int idx = Collections.binarySearch(alerts, new Alert(currentPrice, true),
                Comparator.comparingDouble(Alert::getTargetPrice));
        if (idx < 0) idx = -(idx + 1);

        List<Alert> triggered = alerts.subList(0, idx);

        assertEquals(2, triggered.size());
        assertTrue(triggered.stream().allMatch(a -> a.getTargetPrice() <= currentPrice));
    }
}
