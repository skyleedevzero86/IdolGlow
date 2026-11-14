package com.sleekydz86.idolglow.subscription.application.port.out

interface ProductRecommendationMailMemberPort {
    fun findEmail(userId: Long): String?

    fun hasUserSurvey(userId: Long): Boolean
}
