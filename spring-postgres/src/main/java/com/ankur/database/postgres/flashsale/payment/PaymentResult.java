package com.ankur.database.postgres.flashsale.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResult {
    private final String paymentId;
    private final boolean success;
}