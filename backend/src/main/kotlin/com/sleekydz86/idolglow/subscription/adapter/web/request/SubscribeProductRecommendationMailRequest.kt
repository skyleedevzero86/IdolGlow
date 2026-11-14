package com.sleekydz86.idolglow.subscription.adapter.web.request

import com.sleekydz86.idolglow.subscription.application.dto.SubscribeProductRecommendationMailCommand
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import jakarta.validation.constraints.AssertTrue

data class SubscribeProductRecommendationMailRequest(
    @field:AssertTrue(message = "광고성 정보 수신 동의가 필요합니다.")
    val agreedToAdvertising: Boolean = false,
    val preferredBasis: ProductRecommendationMailPreferredBasis? = null,
    val consentSource: ProductRecommendationMailConsentSource? = null,
) {
    fun toCommand(userId: Long): SubscribeProductRecommendationMailCommand =
        SubscribeProductRecommendationMailCommand(
            userId = userId,
            agreedToAdvertising = agreedToAdvertising,
            preferredBasis = preferredBasis ?: ProductRecommendationMailPreferredBasis.AUTO,
            consentSource = consentSource ?: ProductRecommendationMailConsentSource.MYPAGE,
        )
}
