package com.ankur.database.postgres.flashsale.payment;

public interface PaymentGateway {
    PaymentResult charge(Long userId, java.math.BigDecimal amount);
    void refund(String paymentId);
}