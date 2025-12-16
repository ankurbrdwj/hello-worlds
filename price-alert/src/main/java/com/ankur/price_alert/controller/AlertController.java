package com.ankur.price_alert.controller;

import com.ankur.price_alert.dto.AlertRequest;
import com.ankur.price_alert.dto.AlertResponse;
import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.PriceAlertRepository;
import com.ankur.price_alert.service.AlertMatchingService;
import com.ankur.price_alert.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final PriceAlertRepository alertRepository;
    private final UserService userService;
    private final AlertMatchingService alertMatchingService;

    public AlertController(PriceAlertRepository alertRepository,
                          UserService userService,
                          AlertMatchingService alertMatchingService) {
        this.alertRepository = alertRepository;
        this.userService = userService;
        this.alertMatchingService = alertMatchingService;
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@RequestBody AlertRequest request) {
        // Validate user exists
        User user = userService.getById(request.getUserId());

        // Check if user can create more alerts
        if (!userService.canCreateMoreAlerts(request.getUserId())) {
            return ResponseEntity.badRequest().build();
        }

        // Create alert
        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .symbol(request.getSymbol().toUpperCase())
                .alertType(request.getAlertType())
                .threshold(request.getThreshold())
                .upperThreshold(request.getUpperThreshold())
                .status(AlertStatus.ACTIVE)
                .oneTime(request.isOneTime())
                .maxTriggers(request.getMaxTriggers() > 0 ? request.getMaxTriggers() : 1)
                .triggerCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        PriceAlert savedAlert = alertRepository.save(alert);

        // Invalidate cache for this symbol
        alertMatchingService.invalidateCacheForSymbol(alert.getSymbol());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AlertResponse.fromAlert(savedAlert));
    }

    // ==================== READ ====================

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> ResponseEntity.ok(AlertResponse.fromAlert(alert)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AlertResponse>> getAlertsByUser(@PathVariable Long userId) {
        List<AlertResponse> alerts = alertRepository.findByUserId(userId).stream()
                .map(AlertResponse::fromAlert)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<AlertResponse>> getActiveAlertsByUser(@PathVariable Long userId) {
        List<AlertResponse> alerts = alertRepository.findByUserIdAndStatus(userId, AlertStatus.ACTIVE).stream()
                .map(AlertResponse::fromAlert)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<AlertResponse>> getAlertsBySymbol(@PathVariable String symbol) {
        List<AlertResponse> alerts = alertRepository.findBySymbolAndStatus(symbol.toUpperCase(), AlertStatus.ACTIVE).stream()
                .map(AlertResponse::fromAlert)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    // ==================== UPDATE ====================

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AlertResponse> deactivateAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setStatus(AlertStatus.PAUSED);
                    PriceAlert saved = alertRepository.save(alert);
                    alertMatchingService.invalidateCacheForSymbol(alert.getSymbol());
                    return ResponseEntity.ok(AlertResponse.fromAlert(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AlertResponse> activateAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setStatus(AlertStatus.ACTIVE);
                    PriceAlert saved = alertRepository.save(alert);
                    alertMatchingService.invalidateCacheForSymbol(alert.getSymbol());
                    return ResponseEntity.ok(AlertResponse.fromAlert(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    String symbol = alert.getSymbol();
                    alertRepository.delete(alert);
                    alertMatchingService.invalidateCacheForSymbol(symbol);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== CACHE STATS ====================

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(alertMatchingService.getCacheStats());
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Void> invalidateCache() {
        alertMatchingService.invalidateAllCache();
        return ResponseEntity.ok().build();
    }

    // ==================== EXCEPTION HANDLING ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}