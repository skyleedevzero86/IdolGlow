package com.sleekydz86.idolglow.subscription.domain

data class SubscriptionDispatchRecipientCounts(
    val sentCount: Long = 0,
    val failedCount: Long = 0,
    val skippedCount: Long = 0,
) {
    fun plus(
        status: SubscriptionDispatchRecipientStatus,
        count: Long,
    ): SubscriptionDispatchRecipientCounts =
        when (status) {
            SubscriptionDispatchRecipientStatus.SENT -> copy(sentCount = sentCount + count)
            SubscriptionDispatchRecipientStatus.FAILED -> copy(failedCount = failedCount + count)
            SubscriptionDispatchRecipientStatus.SKIPPED -> copy(skippedCount = skippedCount + count)
        }

    companion object {
        val EMPTY = SubscriptionDispatchRecipientCounts()
    }
}
