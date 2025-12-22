package com.ankur.price_alert.controller;

import com.ankur.price_alert.service.PriceFeedSimulator;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final PriceFeedSimulator simulator;

    public SimulatorController(PriceFeedSimulator simulator) {
        this.simulator = simulator;
    }

    /**
     * Send a single price update (use with hey for load testing)
     * Example: hey -n 10000 -c 100 http://localhost:8080/api/simulator/tick
     */
    @PostMapping("/tick")
    public Map<String, Object> tick() {
        simulator.sendPriceUpdate();
        return Map.of("status", "sent", "prices", simulator.getCurrentPrices());
    }

    /**
     * Send burst of N messages
     * Example: curl -X POST "http://localhost:8080/api/simulator/burst?count=1000"
     */
    @PostMapping("/burst")
    public Map<String, Object> burst(@RequestParam(defaultValue = "1000") int count) {
        long start = System.currentTimeMillis();
        simulator.sendBurst(count);
        long duration = System.currentTimeMillis() - start;
        return Map.of(
                "status", "complete",
                "count", count,
                "durationMs", duration,
                "throughput", count * 1000 / Math.max(duration, 1) + " msg/sec"
        );
    }

    /**
     * Run load test at specified rate
     * Example: curl -X POST "http://localhost:8080/api/simulator/loadtest?rate=500&duration=10"
     */
    @PostMapping("/loadtest")
    public Map<String, Object> loadTest(
            @RequestParam(defaultValue = "100") int rate,
            @RequestParam(defaultValue = "10") int duration) {
        simulator.loadTest(rate, duration);
        return Map.of(
                "status", "complete",
                "targetRate", rate + " msg/sec",
                "duration", duration + " seconds",
                "expectedTotal", rate * duration
        );
    }

    /**
     * Start continuous 1/sec feed
     */
    @PostMapping("/start")
    public Map<String, String> start() {
        simulator.startContinuousFeed();
        return Map.of("status", "started", "rate", "1 msg/sec");
    }

    /**
     * Stop continuous feed
     */
    @PostMapping("/stop")
    public Map<String, String> stop() {
        simulator.stopContinuousFeed();
        return Map.of("status", "stopped");
    }

    /**
     * Get current simulated prices
     */
    @GetMapping("/prices")
    public Map<String, Double> prices() {
        return simulator.getCurrentPrices();
    }
}