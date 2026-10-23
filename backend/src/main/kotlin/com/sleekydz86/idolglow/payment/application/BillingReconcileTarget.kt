package com.sleekydz86.idolglow.payment.application

enum class BillingReconcileOutcome {
    DONE,
    FAILED,
    PENDING,
    AMOUNT_MISMATCH,
    SKIPPED,
}

data class BillingReconcileApplyResult(
    val outcome: BillingReconcileOutcome,
    val userId: Long? = null,
    val failureMessage: String? = null,
    val amount: Long? = null,
)
