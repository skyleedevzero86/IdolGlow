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

    fun findUserSurvey(userId: Long): UserSurveyResponse {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("ID가 $userId 인 사용자를 찾을 수 없습니다.")

        val survey = userSurveyRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자 설문 정보를 찾을 수 없습니다.")

        return UserSurveyResponse.from(survey)
    }
}
