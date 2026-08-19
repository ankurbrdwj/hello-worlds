package com.ankur.database.postgres.flashsale.service;

import com.ankur.database.postgres.flashsale.entity.Inventory;
import com.ankur.database.postgres.flashsale.entity.InventoryStatus;
import com.ankur.database.postgres.flashsale.entity.Order;
import com.ankur.database.postgres.flashsale.payment.PaymentGateway;
import com.ankur.database.postgres.flashsale.payment.PaymentResult;
import com.ankur.database.postgres.flashsale.payment.StubPaymentGateway;
import com.ankur.database.postgres.flashsale.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FlashSaleSagaService {

    private final FlashSaleService flashSaleService;
    private final InventoryRepository inventoryRepository;
    private final PaymentGateway paymentGateway;

    // ── Saga Orchestrator ────────────────────────────────────────────────────
    //
    // NOT @Transactional — this method must NOT open a DB transaction.
    // Each step opens and commits its own short transaction independently.
    //
    // Step 1 (pick)     → TX opens, row locked + reserved, TX commits   (~2ms)
    // Step 2 (payment)  → external HTTP call, zero DB connection held   (~200ms)
    // Step 3 (purchase) → TX opens, row marked sold + order created, TX commits (~2ms)
    //
    // If this entire method were @Transactional, the DB connection would be
    // held open for the full 200ms+ payment call × N concurrent users = exhaustion.
    public SagaOutcome execute(String sku, Long userId, BigDecimal price) {

        // ── Step 1: PICK (Transaction 1) ─────────────────────────────────────
        // Short TX: SKIP LOCKED grabs one row, marks RESERVED, commits.
        // After this returns, the DB connection is fully released.
        Long inventoryId;
        try {
            inventoryId = flashSaleService.pick(sku, userId);
            System.out.printf("[Saga] Step 1 DONE — reserved inventoryId=%d for userId=%d%n", inventoryId, userId);
        } catch (FlashSaleService.SoldOutException e) {
            return SagaOutcome.soldOut(e.getMessage());
        }

        // ── Step 2: PAYMENT (No transaction, no DB connection held) ──────────
        // Slow external call. If it fails we run compensation: release the
        // reservation so another buyer can claim the unit.
        PaymentResult payment;
        try {
            payment = paymentGateway.charge(userId, price);
            System.out.printf("[Saga] Step 2 DONE — payment %s charged%n", payment.getPaymentId());
        } catch (StubPaymentGateway.PaymentFailedException e) {
            // ── Compensation for Step 1 ───────────────────────────────────────
            // Payment never happened, so just release the reservation.
            releaseReservation(inventoryId);
            System.out.printf("[Saga] Step 2 FAILED — released inventoryId=%d back to pool%n", inventoryId);
            return SagaOutcome.paymentFailed(e.getMessage());
        }

        // ── Step 3: PURCHASE (Transaction 2) ─────────────────────────────────
        // Short TX: marks row SOLD, inserts into flash_orders, commits.
        // Can fail if TTL cron reclaimed the reservation between Step 1 and now.
        try {
            Order order = flashSaleService.purchase(inventoryId, userId);
            System.out.printf("[Saga] Step 3 DONE — orderId=%d created%n", order.getId());
            return SagaOutcome.success(order.getId(), payment.getPaymentId());
        } catch (FlashSaleService.ReservationExpiredException e) {
            // ── Compensation for Step 2 ───────────────────────────────────────
            // Inventory was reclaimed by TTL cron. We must refund the payment
            // since we cannot deliver the item.
            paymentGateway.refund(payment.getPaymentId());
            System.out.printf("[Saga] Step 3 FAILED — refunded payment %s%n", payment.getPaymentId());
            return SagaOutcome.reservationExpired(payment.getPaymentId());
        }
    }

    // Compensation transaction — called when payment fails after a successful pick
    @Transactional
    public void releaseReservation(Long inventoryId) {
        inventoryRepository.findById(inventoryId).ifPresent(unit -> {
            unit.setStatus(InventoryStatus.AVAILABLE);
            unit.setReservedBy(null);
            unit.setReservedAt(null);
        });
    }

    // ── Outcome types ────────────────────────────────────────────────────────

    public sealed interface SagaOutcome
            permits SagaOutcome.Success, SagaOutcome.SoldOut,
                    SagaOutcome.PaymentFailed, SagaOutcome.ReservationExpired {

        record Success(Long orderId, String paymentId) implements SagaOutcome {}
        record SoldOut(String reason) implements SagaOutcome {}
        record PaymentFailed(String reason) implements SagaOutcome {}
        record ReservationExpired(String refundedPaymentId) implements SagaOutcome {}

        static SagaOutcome success(Long orderId, String paymentId) { return new Success(orderId, paymentId); }
        static SagaOutcome soldOut(String reason)                   { return new SoldOut(reason); }
        static SagaOutcome paymentFailed(String reason)             { return new PaymentFailed(reason); }
        static SagaOutcome reservationExpired(String paymentId)     { return new ReservationExpired(paymentId); }
    }
}