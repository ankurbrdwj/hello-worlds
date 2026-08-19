package com.ankur.database.postgres.flashsale.repository;

import com.ankur.database.postgres.flashsale.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Transaction 1 — PICK
    // SKIP LOCKED: skip any row already locked by another transaction instead of waiting.
    // Returns empty if all units are reserved/sold (sold-out path).
    @Query(value = """
            SELECT * FROM inventory
            WHERE sku = :sku
              AND status = 'AVAILABLE'
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<Inventory> pickOneForUpdate(@Param("sku") String sku);

    // TTL garbage collection — reclaim reservations older than :cutoff
    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.status = com.ankur.database.postgres.flashsale.entity.InventoryStatus.AVAILABLE,
                i.reservedBy = null,
                i.reservedAt = null
            WHERE i.status = com.ankur.database.postgres.flashsale.entity.InventoryStatus.RESERVED
              AND i.reservedAt < :cutoff
            """)
    int releaseExpiredReservations(@Param("cutoff") OffsetDateTime cutoff);
}