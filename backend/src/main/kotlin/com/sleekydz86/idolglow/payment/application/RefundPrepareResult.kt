package com.sleekydz86.idolglow.payment.application

data class RefundPrepareResult(
    val paymentId: Long,
    val orderId: String? = null,
    val refundId: Long? = null,
    val paymentKey: String? = null,
    val idempotencyKey: String? = null,
    val cancelReason: String? = null,
    val needsTossCancel: Boolean = false,
)
