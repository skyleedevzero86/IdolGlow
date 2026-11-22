package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailConsentReminderPlan
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailMemberPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProductRecommendationMailConsentReminderTxService(
    private val productRecommendationMailSubscriptionPort: ProductRecommendationMailSubscriptionPort,
    private val productRecommendationMailMemberPort: ProductRecommendationMailMemberPort,
    private val unsubscribeTokenService: UnsubscribeTokenService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun plan(now: LocalDateTime): List<ProductRecommendationMailConsentReminderPlan> =
        productRecommendationMailSubscriptionPort
            .findAllConsentReminderDue(dueThreshold(now))
            .mapNotNull { subscription ->
                productRecommendationMailMemberPort.findMailableEmail(subscription.userId)?.let { email ->
                    ProductRecommendationMailConsentReminderPlan(
                        subscriptionId = subscription.id,
                        email = email,
                        consentedAt = subscription.consentedAt,
                        unsubscribeToken = unsubscribeTokenService.issue(subscription),
                    )
                }
            }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordReminded(
        subscriptionId: Long,
        now: LocalDateTime,
    ) {
        val subscription = productRecommendationMailSubscriptionPort.findById(subscriptionId) ?: return
        if (!subscription.active) return
        subscription.recordConsentReminded(now)
        productRecommendationMailSubscriptionPort.save(subscription)
    }

    companion object {
        fun dueThreshold(now: LocalDateTime): LocalDateTime =
            now.minusYears(ProductRecommendationMailSubscription.CONSENT_REMINDER_INTERVAL_YEARS)
    }
}
