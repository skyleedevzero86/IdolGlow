package com.sleekydz86.idolglow.payment.application

data class BillingKeyDeletionPrepared(
    val methodId: Long,
    val userId: Long,
    val billingKey: String?,
    val callToss: Boolean,
    val notify: Boolean,
)

enum class BillingKeyDeletionOutcome {
    DELETED,
    PENDING,
    FAILED,
    SKIPPED,
}

data class BillingKeyDeletionApplied(
    val outcome: BillingKeyDeletionOutcome,
    val userId: Long? = null,
    val notify: Boolean = false,
)
