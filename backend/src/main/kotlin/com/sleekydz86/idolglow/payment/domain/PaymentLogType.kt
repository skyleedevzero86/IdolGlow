package com.sleekydz86.idolglow.payment.domain

enum class PaymentLogType {
    CONFIRM_REQUEST,
    CONFIRM_RESPONSE,
    CONFIRM_ERROR,
    CANCEL_REQUEST,
    CANCEL_RESPONSE,
    CANCEL_ERROR,
    WEBHOOK_RECEIVED,
    WEBHOOK_REJECTED,
    MOCK_WEBHOOK,
    SYSTEM,
}
