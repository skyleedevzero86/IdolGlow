package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience

interface EmailSubscriptionPort {
    fun findAllByLatest(): List<EmailSubscription>
    fun findByEmail(email: String): EmailSubscription?
    fun findActiveEmailsByAudience(audience: SubscriptionAudience): List<String>
    fun save(subscription: EmailSubscription): EmailSubscription
    fun count(): Long
    fun countActive(): Long
    fun countActiveByAudience(audience: SubscriptionAudience): Long
}
