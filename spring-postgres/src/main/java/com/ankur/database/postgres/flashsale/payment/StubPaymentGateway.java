package com.ankur.database.postgres.flashsale.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Simulates a real payment gateway: ~200ms latency, occasional failure
@Component
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(Long userId, BigDecimal amount) {
        simulateNetworkLatency();

        // Simulate 10% payment failure rate
        if (Math.random() < 0.10) {
            throw new PaymentFailedException("Card declined for userId=" + userId);
        }

        return new PaymentResult(UUID.randomUUID().toString(), true);
    }

    @Override
    public void refund(String paymentId) {
        simulateNetworkLatency();
        System.out.printf("[PaymentGateway] Refunded payment %s%n", paymentId);
    }

    private void simulateNetworkLatency() {
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String msg) { super(msg); }
    }
}