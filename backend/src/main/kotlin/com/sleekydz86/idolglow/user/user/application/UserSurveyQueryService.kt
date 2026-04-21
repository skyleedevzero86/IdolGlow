package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class UserSurveyQueryService(
    private val userSurveyRepository: UserSurveyRepository,
    private val userRepository: UserRepository,
) {
    fun findUserSurveyIfPresent(userId: Long): UserSurveyResponse? {
        userRepository.findById(userId) ?: return null
        val survey = userSurveyRepository.findByUserId(userId) ?: return null
        return UserSurveyResponse.from(survey)
    }
    fun findUserSurvey(userId: Long): UserSurveyResponse =
        findUserSurveyIfPresent(userId)
            ?: throw IllegalArgumentException("사용자 설문 정보를 찾을 수 없습니다.")
}
