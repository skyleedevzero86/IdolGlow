package com.sleekydz86.idolglow.subscription.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlValue
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailBasisPreview
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSettingResult
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis

data class RecommendationMailSettingGraphQlResponse(
    val active: Boolean,
    val preferredBasis: ProductRecommendationMailPreferredBasis,
    val consentSource: ProductRecommendationMailConsentSource?,
    val consentedAt: String?,
    val unsubscribedAt: String?,
    val hasUserSurvey: Boolean,
    val expectedBasis: ProductRecommendationMailBasisPreview,
    val maskedEmail: String?,
) {
    companion object {
        fun from(result: ProductRecommendationMailSettingResult): RecommendationMailSettingGraphQlResponse =
            RecommendationMailSettingGraphQlResponse(
                active = result.active,
                preferredBasis = result.preferredBasis,
                consentSource = result.consentSource,
                consentedAt = result.consentedAt.asGraphQlValue(),
                unsubscribedAt = result.unsubscribedAt.asGraphQlValue(),
                hasUserSurvey = result.hasUserSurvey,
                expectedBasis = result.expectedBasis,
                maskedEmail = result.maskedEmail,
            )
    }
}
