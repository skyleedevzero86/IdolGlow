package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailBasis
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailItem
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailPlan
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailMemberPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailProductPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProductRecommendationMailDispatchTxService(
    private val productRecommendationMailSubscriptionPort: ProductRecommendationMailSubscriptionPort,
    private val productRecommendationMailMemberPort: ProductRecommendationMailMemberPort,
    private val productRecommendationMailProductPort: ProductRecommendationMailProductPort,
    private val unsubscribeTokenService: UnsubscribeTokenService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun countRecipients(): Long = recipients(emptySet()).size.toLong()

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun plan(alreadySent: Set<String>): List<ProductRecommendationMailPlan> {
        val popular by lazy { productRecommendationMailProductPort.findPopular(CANDIDATE_SIZE) }
        return recipients(alreadySent).map { (subscription, email) ->
            val personalized =
                if (wantsPersonalized(subscription)) {
                    productRecommendationMailProductPort.findPersonalized(subscription.userId, CANDIDATE_SIZE)
                } else {
                    null
                }
            val personalizedItems =
                personalized?.takeUnless { it.fellBackToPopular || it.items.isEmpty() }?.items
            ProductRecommendationMailPlan(
                subscriptionId = subscription.id,
                email = email,
                basis =
                    if (personalizedItems != null) {
                        ProductRecommendationMailBasis.PERSONALIZED
                    } else {
                        ProductRecommendationMailBasis.POPULAR
                    },
                surveyFallback = personalized != null && personalizedItems == null,
                items = select(personalizedItems ?: popular, subscription.lastSentProductIdSet()),
                unsubscribeToken = unsubscribeTokenService.issue(subscription),
            )
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordSent(
        subscriptionId: Long,
        productIds: List<Long>,
        now: LocalDateTime,
    ) {
        val subscription = productRecommendationMailSubscriptionPort.findById(subscriptionId) ?: return
        subscription.recordSent(productIds, now)
        productRecommendationMailSubscriptionPort.save(subscription)
    }

    private fun recipients(alreadySent: Set<String>): List<Pair<ProductRecommendationMailSubscription, String>> =
        productRecommendationMailSubscriptionPort
            .findAllActive()
            .mapNotNull { subscription ->
                productRecommendationMailMemberPort
                    .findMailableEmail(subscription.userId)
                    ?.takeUnless { it in alreadySent }
                    ?.let { subscription to it }
            }.distinctBy { it.second.lowercase() }

    private fun wantsPersonalized(subscription: ProductRecommendationMailSubscription): Boolean =
        subscription.preferredBasis == ProductRecommendationMailPreferredBasis.AUTO &&
            productRecommendationMailMemberPort.hasUserSurvey(subscription.userId)

    companion object {
        const val CANDIDATE_SIZE = 8
        const val MAIL_ITEM_SIZE = 5
        const val MIN_ITEMS_AFTER_EXCLUSION = 3

        fun select(
            candidates: List<ProductRecommendationMailItem>,
            lastSentProductIds: Set<Long>,
        ): List<ProductRecommendationMailItem> {
            val unique = candidates.distinctBy { it.productId }
            val fresh = unique.filterNot { it.productId in lastSentProductIds }
            val chosen = if (fresh.size < MIN_ITEMS_AFTER_EXCLUSION) unique else fresh
            return chosen.take(MAIL_ITEM_SIZE)
        }
    }
}
