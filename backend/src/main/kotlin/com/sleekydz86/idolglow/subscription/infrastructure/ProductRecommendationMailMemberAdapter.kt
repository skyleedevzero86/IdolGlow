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

    override fun findMailableEmail(userId: Long): String? =
        userRepository
            .findById(userId)
            ?.takeIf { it.isPlatformActive() }
            ?.email
            ?.trim()
            ?.takeIf { it.length <= EMAIL_MAX_LENGTH && EMAIL_PATTERN.matches(it) }

    companion object {
        private const val EMAIL_MAX_LENGTH = 255
        private val EMAIL_PATTERN = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$""")
    }
}
