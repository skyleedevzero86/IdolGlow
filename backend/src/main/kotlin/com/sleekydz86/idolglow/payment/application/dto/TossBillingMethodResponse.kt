package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import java.time.LocalDateTime

data class TossBillingMethodResponse(
    val id: Long,
    val customerKey: String,
    val cardCompany: String?,
    val cardNumber: String?,
    val cardType: String?,
    val ownerType: String?,
    val active: Boolean,
    val authenticatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: TossBillingMethod) =
            TossBillingMethodResponse(
                id = entity.id,
                customerKey = entity.customerKey,
                cardCompany = entity.cardCompany,
                cardNumber = entity.cardNumber,
                cardType = entity.cardType,
                ownerType = entity.ownerType,
                active = entity.active,
                authenticatedAt = entity.authenticatedAt,
            )
    }
}
