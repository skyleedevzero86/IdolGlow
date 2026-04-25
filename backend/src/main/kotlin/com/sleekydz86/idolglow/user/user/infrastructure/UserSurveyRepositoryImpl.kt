package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import org.springframework.stereotype.Repository

@Repository
class UserSurveyRepositoryImpl(
    private val userSurveyJpaRepository: UserSurveyJpaRepository
): UserSurveyRepository {

    override fun findByUserId(userId: Long): UserSurvey? =
        userSurveyJpaRepository.findFirstByUserIdOrderByIdDesc(userId)

    override fun existsByUserId(userId: Long): Boolean =
        userSurveyJpaRepository.existsByUserId(userId)

    override fun save(userSurvey: UserSurvey): UserSurvey =
        userSurveyJpaRepository.save(userSurvey)

    override fun deleteByUserId(userId: Long) {
        userSurveyJpaRepository.deleteByUserId(userId)
    }
}
