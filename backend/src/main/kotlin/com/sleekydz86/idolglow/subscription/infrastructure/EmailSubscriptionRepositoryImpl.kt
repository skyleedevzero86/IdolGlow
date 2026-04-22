package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class EmailSubscriptionRepositoryImpl(
    private val emailSubscriptionJpaRepository: EmailSubscriptionJpaRepository,
) : EmailSubscriptionPort {

    override fun findAllByLatest(): List<EmailSubscription> =
        emailSubscriptionJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("subscribedAt"),
                Sort.Order.desc("createdAt"),
            )
        )

    override fun findByEmail(email: String): EmailSubscription? =
        emailSubscriptionJpaRepository.findByEmail(email.trim().lowercase())

    override fun findActiveEmailsByAudience(audience: SubscriptionAudience): List<String> =
        when (audience) {
            SubscriptionAudience.NEWSLETTER ->
                emailSubscriptionJpaRepository.findAllByActiveTrueAndSubscribedNewslettersTrueOrderBySubscribedAtDesc()
            SubscriptionAudience.WEBZINE_ISSUE ->
                emailSubscriptionJpaRepository.findAllByActiveTrueAndSubscribedIssuesTrueOrderBySubscribedAtDesc()
        }.map { it.email }

    override fun save(subscription: EmailSubscription): EmailSubscription =
        emailSubscriptionJpaRepository.save(subscription)

    override fun count(): Long =
        emailSubscriptionJpaRepository.count()

    override fun countActive(): Long =
        emailSubscriptionJpaRepository.countByActiveTrue()

    override fun countActiveByAudience(audience: SubscriptionAudience): Long =
        when (audience) {
            SubscriptionAudience.NEWSLETTER -> emailSubscriptionJpaRepository.countByActiveTrueAndSubscribedNewslettersTrue()
            SubscriptionAudience.WEBZINE_ISSUE -> emailSubscriptionJpaRepository.countByActiveTrueAndSubscribedIssuesTrue()
        }
}
