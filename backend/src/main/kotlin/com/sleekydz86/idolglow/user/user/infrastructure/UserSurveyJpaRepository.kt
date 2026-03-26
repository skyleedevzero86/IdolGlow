package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import org.springframework.data.jpa.repository.JpaRepository

interface UserSurveyJpaRepository: JpaRepository<UserSurvey, Long> {
    fun findByUserId(userId: Long): UserSurvey?
    fun existsByUserId(userId: Long): Boolean
}