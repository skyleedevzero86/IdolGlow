package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.application.BillingDefaults
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class StartTossBillingSubscriptionRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val orderName: String = BillingDefaults.DEFAULT_ORDER_NAME,
    val chargeImmediately: Boolean = true,
)
