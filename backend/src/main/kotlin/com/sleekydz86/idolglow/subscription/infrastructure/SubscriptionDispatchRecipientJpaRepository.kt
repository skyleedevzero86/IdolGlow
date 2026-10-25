package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionDispatchRecipientJpaRepository : JpaRepository<SubscriptionDispatchRecipient, Long> {
    fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient>

    fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient?
}
