package com.sleekydz86.idolglow.user.user.application

import com.sleekydz86.idolglow.user.user.application.dto.UpsertUserSurveyCommand
import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class UserSurveyCommandService(
    private val userRepository: UserRepository,
    private val userSurveyRepository: UserSurveyRepository,
) {

    fun saveUserSurvey(userId: Long, command: UpsertUserSurveyCommand): UserSurveyUpsertResponse {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("ID가 $userId 인 사용자를 찾을 수 없습니다.")

        val survey = userSurveyRepository.findByUserId(userId)
            ?.apply { update(command) }
            ?: userSurveyRepository.save(
                UserSurvey.of(
                    user = user,
                    concept = command.concept,
                    idolName = command.idolName,
                    visitStartDate = command.visitStartDate,
                    visitEndDate = command.visitEndDate,
                    places = command.places
                )
            )

        return UserSurveyUpsertResponse(survey.id)
    }
}
