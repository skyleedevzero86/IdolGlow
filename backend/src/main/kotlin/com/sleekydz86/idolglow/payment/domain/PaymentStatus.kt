package com.sleekydz86.idolglow.payment.domain

enum class PaymentStatus {
    PENDING,
    CONFIRMING,
    SUCCEEDED,
    REFUNDING,
    FAILED,
    CANCELED,
    EXPIRED,
    REFUNDED,
    PARTIAL_CANCELED,
}
