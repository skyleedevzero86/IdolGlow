package com.sleekydz86.idolglow.subscription.domain

interface EmailSubscriptionRepository {
    fun findAllByLatest(): List<EmailSubscription>
    fun findByEmail(email: String): EmailSubscription?
    fun save(subscription: EmailSubscription): EmailSubscription
    fun count(): Long
    fun countActive(): Long
    fun countActiveByAudience(audience: SubscriptionAudience): Long
}
