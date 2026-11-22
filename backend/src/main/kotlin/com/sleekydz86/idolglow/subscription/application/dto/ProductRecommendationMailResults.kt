package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import java.time.LocalDateTime

enum class ProductRecommendationMailBasisPreview {
    PERSONALIZED,
    POPULAR,
}

data class SubscribeProductRecommendationMailCommand(
    val userId: Long,
    val agreedToAdvertising: Boolean,
    val preferredBasis: ProductRecommendationMailPreferredBasis,
    val consentSource: ProductRecommendationMailConsentSource,
)

data class ProductRecommendationMailSettingResult(
    val active: Boolean,
    val preferredBasis: ProductRecommendationMailPreferredBasis,
    val consentSource: ProductRecommendationMailConsentSource?,
    val consentedAt: LocalDateTime?,
    val unsubscribedAt: LocalDateTime?,
    val hasUserSurvey: Boolean,
    val expectedBasis: ProductRecommendationMailBasisPreview,
    val maskedEmail: String?,
) {
    companion object {
        fun of(
            subscription: ProductRecommendationMailSubscription?,
            hasUserSurvey: Boolean,
            email: String?,
        ): ProductRecommendationMailSettingResult {
            val preferredBasis = subscription?.preferredBasis ?: ProductRecommendationMailPreferredBasis.AUTO
            return ProductRecommendationMailSettingResult(
                active = subscription?.active ?: false,
                preferredBasis = preferredBasis,
                consentSource = subscription?.consentSource,
                consentedAt = subscription?.consentedAt,
                unsubscribedAt = subscription?.unsubscribedAt,
                hasUserSurvey = hasUserSurvey,
                expectedBasis =
                    if (preferredBasis == ProductRecommendationMailPreferredBasis.AUTO && hasUserSurvey) {
                        ProductRecommendationMailBasisPreview.PERSONALIZED
                    } else {
                        ProductRecommendationMailBasisPreview.POPULAR
                    },
                maskedEmail = email?.let(ProductRecommendationMailEmailMasker::mask),
            )
        }
    }
}

data class ProductRecommendationMailUnsubscribePreviewResult(
    val valid: Boolean,
    val maskedEmail: String?,
    val alreadyUnsubscribed: Boolean,
) {
    companion object {
        val INVALID =
            ProductRecommendationMailUnsubscribePreviewResult(
                valid = false,
                maskedEmail = null,
                alreadyUnsubscribed = false,
            )
    }
}

data class ProductRecommendationMailUnsubscribeResult(
    val processed: Boolean,
    val message: String,
) {
    companion object {
        val ACCEPTED =
            ProductRecommendationMailUnsubscribeResult(
                processed = true,
                message = "수신 거부 요청을 처리했습니다. 유효한 링크라면 더 이상 상품 추천 메일이 발송되지 않습니다.",
            )
    }
}

data class ProductRecommendationMailUnsubscribeNotice(
    val subscriptionId: Long,
    val email: String,
    val unsubscribedAt: LocalDateTime,
)

data class ProductRecommendationMailUnsubscribeChange(
    val setting: ProductRecommendationMailSettingResult,
    val notice: ProductRecommendationMailUnsubscribeNotice?,
)

object ProductRecommendationMailEmailMasker {
    private const val VISIBLE_PREFIX = 2
    private const val MASK = "***"

    fun mask(email: String): String {
        val trimmed = email.trim()
        val at = trimmed.lastIndexOf('@')
        if (at <= 0) return MASK
        val local = trimmed.substring(0, at)
        val visible = local.take(if (local.length > VISIBLE_PREFIX) VISIBLE_PREFIX else 1)
        return "$visible$MASK${trimmed.substring(at)}"
    }
}
