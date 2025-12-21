package com.ankur.price_alert.controller;

import com.ankur.price_alert.dto.AlertRequest;
import com.ankur.price_alert.dto.AlertResponse;
import com.ankur.price_alert.exception.AlertNotFoundException;
import com.ankur.price_alert.exception.InvalidAlertConfigurationException;
import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.PriceAlertRepository;
import com.ankur.price_alert.service.AlertCacheManager;
import com.ankur.price_alert.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for alert management.
 * Follows Dependency Inversion Principle - depends on AlertCacheManager interface.
 * Uses custom exception hierarchy for error handling.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final PriceAlertRepository alertRepository;
    private final UserService userService;
    private final AlertCacheManager alertCacheManager;

    public AlertController(PriceAlertRepository alertRepository,
                          UserService userService,
                          AlertCacheManager alertCacheManager) {
        this.alertRepository = alertRepository;
        this.userService = userService;
        this.alertCacheManager = alertCacheManager;
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@RequestBody AlertRequest request) {
        // Validate user exists (throws UserNotFoundException if not found)
        User user = userService.getById(request.getUserId());

        // Validate alert quota (throws AlertQuotaExceededException if exceeded)
        userService.validateAlertQuota(request.getUserId());

        // Validate alert configuration
        validateAlertRequest(request);

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
        alertCacheManager.invalidateCacheForSymbol(alert.getSymbol());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AlertResponse.fromAlert(savedAlert));
    }

    private void validateAlertRequest(AlertRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new InvalidAlertConfigurationException("Symbol is required");
        }
        if (request.getAlertType() == null) {
            throw new InvalidAlertConfigurationException("Alert type is required");
        }
        if (request.getThreshold() <= 0) {
            throw new InvalidAlertConfigurationException("Threshold must be positive");
        }
        if (request.getAlertType() == AlertType.PRICE_BETWEEN) {
            if (request.getUpperThreshold() == null || request.getUpperThreshold() <= request.getThreshold()) {
                throw new InvalidAlertConfigurationException(
                        "PRICE_BETWEEN alerts require upperThreshold greater than threshold");
            }
        }
    }

    // ==================== READ ====================

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlert(@PathVariable Long id) {
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));
        return ResponseEntity.ok(AlertResponse.fromAlert(alert));
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
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        alert.setStatus(AlertStatus.PAUSED);
        PriceAlert saved = alertRepository.save(alert);
        alertCacheManager.invalidateCacheForSymbol(alert.getSymbol());

        return ResponseEntity.ok(AlertResponse.fromAlert(saved));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AlertResponse> activateAlert(@PathVariable Long id) {
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        alert.setStatus(AlertStatus.ACTIVE);
        PriceAlert saved = alertRepository.save(alert);
        alertCacheManager.invalidateCacheForSymbol(alert.getSymbol());

        return ResponseEntity.ok(AlertResponse.fromAlert(saved));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        PriceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        String symbol = alert.getSymbol();
        alertRepository.delete(alert);
        alertCacheManager.invalidateCacheForSymbol(symbol);

        return ResponseEntity.noContent().build();
    }

    // ==================== CACHE STATS ====================

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(alertCacheManager.getCacheStats());
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Void> invalidateCache() {
        alertCacheManager.invalidateAllCache();
        return ResponseEntity.ok().build();
    }
}