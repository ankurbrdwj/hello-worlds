package com.ankur.database.postgres.flashsale.controller;

import com.ankur.database.postgres.flashsale.entity.Order;
import com.ankur.database.postgres.flashsale.service.FlashSaleService;
import com.ankur.database.postgres.flashsale.service.FlashSaleSagaService;
import com.ankur.database.postgres.flashsale.service.FlashSaleSagaService.SagaOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final FlashSaleSagaService sagaService;

    // Full saga — pick + payment + purchase in one call
    @PostMapping("/{sku}/buy")
    public ResponseEntity<?> buy(@PathVariable String sku,
                                 @RequestParam Long userId,
                                 @RequestParam(defaultValue = "999.00") BigDecimal price) {
        return switch (sagaService.execute(sku, userId, price)) {
            case SagaOutcome.Success s ->
                    ResponseEntity.ok(Map.of("orderId", s.orderId(), "paymentId", s.paymentId()));
            case SagaOutcome.SoldOut s ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", s.reason()));
            case SagaOutcome.PaymentFailed p ->
                    ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", p.reason()));
            case SagaOutcome.ReservationExpired r ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "reservation expired", "refundedPaymentId", r.refundedPaymentId()));
        };
    }

    // Step 1 — user clicks "Buy Now"
    @PostMapping("/{sku}/pick")
    public ResponseEntity<?> pick(@PathVariable String sku,
                                  @RequestParam Long userId) {
        try {
            Long inventoryId = flashSaleService.pick(sku, userId);
            return ResponseEntity.ok(Map.of("inventoryId", inventoryId));
        } catch (FlashSaleService.SoldOutException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Step 2 — called after payment gateway returns success
    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(@RequestParam Long inventoryId,
                                      @RequestParam Long userId) {
        try {
            Order order = flashSaleService.purchase(inventoryId, userId);
            return ResponseEntity.ok(Map.of("orderId", order.getId()));
        } catch (FlashSaleService.ReservationExpiredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}