package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient

interface SubscriptionDispatchRecipientPort {
    fun save(recipient: SubscriptionDispatchRecipient): SubscriptionDispatchRecipient

    fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient>

    fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient?
}
