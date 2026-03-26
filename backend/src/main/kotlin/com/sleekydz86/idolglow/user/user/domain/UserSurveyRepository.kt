package com.sleekydz86.idolglow.user.user.domain

interface UserSurveyRepository {
    fun findByUserId(userId: Long): UserSurvey?
    fun existsByUserId(userId: Long): Boolean
    fun save(userSurvey: UserSurvey): UserSurvey
}