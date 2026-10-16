package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.application.BillingDefaults
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChargeTossBillingRequest(
    @field:Min(BillingDefaults.MIN_CHARGE_AMOUNT)
    val amount: Long = BillingDefaults.MONTHLY_AMOUNT,
    @field:NotBlank
    @field:Size(max = 200)
    val orderName: String = BillingDefaults.DEFAULT_ORDER_NAME,
)
