package com.sleekydz86.idolglow.payment.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class IssueTossBillingKeyRequest(
    @field:NotBlank
    @field:Size(max = 300)
    val authKey: String,
    @field:NotBlank
    @field:Size(max = 100)
    val customerKey: String,
)
