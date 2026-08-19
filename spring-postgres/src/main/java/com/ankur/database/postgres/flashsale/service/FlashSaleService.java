package com.ankur.database.postgres.flashsale.service;

import com.ankur.database.postgres.flashsale.entity.Inventory;
import com.ankur.database.postgres.flashsale.entity.InventoryStatus;
import com.ankur.database.postgres.flashsale.entity.Order;
import com.ankur.database.postgres.flashsale.repository.InventoryRepository;
import com.ankur.database.postgres.flashsale.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    // ── Transaction 1: PICK ──────────────────────────────────────────────────
    // Short transaction (~2ms). Finds one available unit with SKIP LOCKED,
    // marks it reserved, commits. Payment happens AFTER this returns.
    @Transactional
    public Long pick(String sku, Long userId) {
        Inventory unit = inventoryRepository.pickOneForUpdate(sku)
                .orElseThrow(() -> new SoldOutException(sku));

        unit.setStatus(InventoryStatus.RESERVED);
        unit.setReservedBy(userId);
        unit.setReservedAt(OffsetDateTime.now());

        return unit.getId();
    }

    // ── Transaction 2: PURCHASE ──────────────────────────────────────────────
    // Called only after external payment succeeds. Short transaction (~2ms).
    // Fails if TTL cron already reclaimed the reservation.
    @Transactional
    public Order purchase(Long inventoryId, Long userId) {
        Inventory unit = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown inventory id: " + inventoryId));

        // Guard: reservation may have expired and been reclaimed by the cron
        if (unit.getStatus() != InventoryStatus.RESERVED || !userId.equals(unit.getReservedBy())) {
            throw new ReservationExpiredException(inventoryId);
        }

        unit.setStatus(InventoryStatus.SOLD);
        unit.setPurchasedAt(OffsetDateTime.now());

        Order order = new Order();
        order.setInventory(unit);
        order.setUserId(userId);
        return orderRepository.save(order);
    }

    // ── TTL Garbage Collection ───────────────────────────────────────────────
    // Reclaims reservations older than 10 minutes so other buyers can pick them.
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void releaseExpiredReservations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(10);
        int released = inventoryRepository.releaseExpiredReservations(cutoff);
        if (released > 0) {
            System.out.printf("[TTL] Released %d expired reservation(s)%n", released);
        }
    }

    // ── Exceptions ───────────────────────────────────────────────────────────

    public static class SoldOutException extends RuntimeException {
        public SoldOutException(String sku) {
            super("Sold out: " + sku);
        }
    }

    public static class ReservationExpiredException extends RuntimeException {
        public ReservationExpiredException(Long inventoryId) {
            super("Reservation expired or does not belong to this user: inventoryId=" + inventoryId);
        }
    }
}