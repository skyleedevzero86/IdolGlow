package com.sleekydz86.idolglow.payment.domain

enum class PaymentStatus {
    PENDING,
    CONFIRMING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    EXPIRED,
    REFUNDED,
    PARTIAL_CANCELED,
}
