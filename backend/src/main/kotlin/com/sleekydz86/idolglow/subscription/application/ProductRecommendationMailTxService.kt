package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailEmailMasker
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSettingResult
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeChange
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeNotice
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribePreviewResult
import com.sleekydz86.idolglow.subscription.application.dto.SubscribeProductRecommendationMailCommand
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailMemberPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ProductRecommendationMailTxService(
    private val productRecommendationMailSubscriptionPort: ProductRecommendationMailSubscriptionPort,
    private val productRecommendationMailMemberPort: ProductRecommendationMailMemberPort,
    private val unsubscribeTokenService: UnsubscribeTokenService,
) {
    @Transactional(readOnly = true)
    fun findMine(userId: Long): ProductRecommendationMailSettingResult =
        setting(userId, productRecommendationMailSubscriptionPort.findByUserId(userId))

    @Transactional
    fun subscribe(
        command: SubscribeProductRecommendationMailCommand,
        now: LocalDateTime,
    ): ProductRecommendationMailSettingResult {
        require(command.agreedToAdvertising) { "광고성 정보 수신 동의가 필요합니다." }
        requireNotNull(productRecommendationMailMemberPort.findEmail(command.userId)) {
            "메일을 받을 회원 이메일을 찾을 수 없습니다."
        }
        val existing = productRecommendationMailSubscriptionPort.findByUserId(command.userId)
        val saved =
            if (existing == null) {
                productRecommendationMailSubscriptionPort.save(
                    ProductRecommendationMailSubscription.create(
                        userId = command.userId,
                        consentSource = command.consentSource,
                        preferredBasis = command.preferredBasis,
                        now = now,
                    ),
                )
            } else {
                existing.subscribe(command.consentSource, now)
                existing.changeBasis(command.preferredBasis)
                productRecommendationMailSubscriptionPort.save(existing)
            }
        return setting(command.userId, saved)
    }

    @Transactional
    fun unsubscribeByUser(
        userId: Long,
        now: LocalDateTime,
    ): ProductRecommendationMailUnsubscribeChange {
        val subscription = productRecommendationMailSubscriptionPort.findByUserId(userId)
        val notice = subscription?.let { unsubscribe(it, now) }
        return ProductRecommendationMailUnsubscribeChange(
            setting = setting(userId, subscription),
            notice = notice,
        )
    }

    @Transactional(readOnly = true)
    fun preview(token: String): ProductRecommendationMailUnsubscribePreviewResult {
        val subscription =
            unsubscribeTokenService.verify(token)
                ?: return ProductRecommendationMailUnsubscribePreviewResult.INVALID
        return ProductRecommendationMailUnsubscribePreviewResult(
            valid = true,
            maskedEmail =
                productRecommendationMailMemberPort
                    .findEmail(subscription.userId)
                    ?.let(ProductRecommendationMailEmailMasker::mask),
            alreadyUnsubscribed = !subscription.active,
        )
    }

    @Transactional
    fun unsubscribeByToken(
        token: String,
        now: LocalDateTime,
    ): ProductRecommendationMailUnsubscribeNotice? = unsubscribeTokenService.verify(token)?.let { unsubscribe(it, now) }

    private fun unsubscribe(
        subscription: ProductRecommendationMailSubscription,
        now: LocalDateTime,
    ): ProductRecommendationMailUnsubscribeNotice? {
        if (!subscription.active) return null
        subscription.unsubscribe(now)
        productRecommendationMailSubscriptionPort.save(subscription)
        return productRecommendationMailMemberPort.findEmail(subscription.userId)?.let { email ->
            ProductRecommendationMailUnsubscribeNotice(
                subscriptionId = subscription.id,
                email = email,
                unsubscribedAt = now,
            )
        }
    }

    private fun setting(
        userId: Long,
        subscription: ProductRecommendationMailSubscription?,
    ): ProductRecommendationMailSettingResult =
        ProductRecommendationMailSettingResult.of(
            subscription = subscription,
            hasUserSurvey = productRecommendationMailMemberPort.hasUserSurvey(userId),
            email = productRecommendationMailMemberPort.findEmail(userId),
        )
}
