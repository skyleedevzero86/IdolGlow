package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus

interface SubscriptionDispatchRecipientStatusCount {
    val dispatchId: Long
    val status: SubscriptionDispatchRecipientStatus
    val total: Long
}
