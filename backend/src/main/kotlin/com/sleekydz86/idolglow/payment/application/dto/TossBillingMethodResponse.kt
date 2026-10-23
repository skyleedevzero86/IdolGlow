package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.TossBillingKeyDeletionStatus
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
    val isDefault: Boolean,
    val authenticatedAt: LocalDateTime?,
    val deletionStatus: TossBillingKeyDeletionStatus = TossBillingKeyDeletionStatus.ACTIVE,
    val deletionRequestedAt: LocalDateTime? = null,
    val deletionCompletedAt: LocalDateTime? = null,
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
                isDefault = entity.isDefault,
                authenticatedAt = entity.authenticatedAt,
                deletionStatus = entity.deletionStatus,
                deletionRequestedAt = entity.deletionRequestedAt,
                deletionCompletedAt = entity.deletionCompletedAt,
            )
    }
}
