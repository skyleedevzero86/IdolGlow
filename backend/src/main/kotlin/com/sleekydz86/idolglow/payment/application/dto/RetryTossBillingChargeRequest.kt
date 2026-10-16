package com.sleekydz86.idolglow.payment.application.dto

import jakarta.validation.constraints.Positive

data class RetryTossBillingChargeRequest(
    @field:Positive
    val methodId: Long,
    val setAsDefault: Boolean = false,
)
