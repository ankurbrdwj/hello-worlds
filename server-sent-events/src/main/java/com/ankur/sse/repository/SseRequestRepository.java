package com.ankur.sse.repository;

import com.ankur.sse.model.SseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SseRequestRepository extends JpaRepository<SseRequest, Long> {

    // Find pending (undelivered) requests for a specific user
    List<SseRequest> findByUserIdAndDeliveredFalseAndExpiresAtAfter(
            String userId,
            LocalDateTime currentTime
    );

    // Find all pending persistent requests for a user
    List<SseRequest> findByUserIdAndDeliveredFalseAndPersistentTrueAndExpiresAtAfter(
            String userId,
            LocalDateTime currentTime
    );

    // Clean up expired requests
    void deleteByExpiresAtBefore(LocalDateTime currentTime);
}