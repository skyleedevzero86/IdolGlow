package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment

data class TossConfirmPrepareResult(
    val payment: Payment,
    val skipGateway: Boolean,
    val idempotencyKey: String,
)
