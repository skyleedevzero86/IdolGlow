package com.sleekydz86.idolglow.user.user.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class UserSurveyQueryService(
    private val userSurveyRepository: UserSurveyRepository,
    private val userRepository: UserRepository,
) {

    fun findUserSurvey(userId: Long): UserSurveyResponse {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User with id $userId does not exist.")

        val survey = userSurveyRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("UserSurvey does not exist.")

        return UserSurveyResponse.from(survey)
    }
}
