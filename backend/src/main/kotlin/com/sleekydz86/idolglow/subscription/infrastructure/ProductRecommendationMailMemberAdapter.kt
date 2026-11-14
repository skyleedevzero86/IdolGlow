package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailMemberPort
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import org.springframework.stereotype.Component

@Component
class ProductRecommendationMailMemberAdapter(
    private val userRepository: UserRepository,
    private val userSurveyRepository: UserSurveyRepository,
) : ProductRecommendationMailMemberPort {
    override fun findEmail(userId: Long): String? =
        userRepository
            .findById(userId)
            ?.email
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    override fun hasUserSurvey(userId: Long): Boolean = userSurveyRepository.existsByUserId(userId)
}
