package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import org.springframework.data.jpa.repository.JpaRepository

interface EmailSubscriptionJpaRepository : JpaRepository<EmailSubscription, Long> {
    fun findByEmail(email: String): EmailSubscription?
    fun countByActiveTrue(): Long
    fun countByActiveTrueAndSubscribedNewslettersTrue(): Long
    fun countByActiveTrueAndSubscribedIssuesTrue(): Long
}
