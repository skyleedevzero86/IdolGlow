package com.sleekydz86.idolglow.payment.application.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChargeTossBillingRequest(
    @field:Min(100)
    val amount: Long = 4900,
    @field:NotBlank
    @field:Size(max = 200)
    val orderName: String = "IdolGlow 월간 구독",
)
