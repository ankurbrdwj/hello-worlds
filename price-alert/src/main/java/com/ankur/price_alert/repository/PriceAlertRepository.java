package com.ankur.price_alert.repository;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    // Find alerts by symbol and status (used by AlertMatchingService)
    List<PriceAlert> findBySymbolAndStatus(String symbol, AlertStatus status);

    @Query("SELECT a FROM PriceAlert a WHERE a.symbol = :symbol AND a.status = 'ACTIVE'")
    List<PriceAlert> findBySymbolAndStatusActive(@Param("symbol") String symbol);

    // Find alerts by user
    List<PriceAlert> findByUserIdAndStatus(Long userId, AlertStatus status);

    List<PriceAlert> findByUserId(Long userId);

    // Count active alerts for a user
    @Query("SELECT COUNT(a) FROM PriceAlert a WHERE a.user.id = :userId AND a.status = 'ACTIVE'")
    int countActiveAlertsByUserId(@Param("userId") Long userId);

    // Find all active alerts (for cache warming)
    List<PriceAlert> findByStatus(AlertStatus status);

    // Find distinct symbols with active alerts
    @Query("SELECT DISTINCT a.symbol FROM PriceAlert a WHERE a.status = 'ACTIVE'")
    List<String> findDistinctSymbolsWithActiveAlerts();
}