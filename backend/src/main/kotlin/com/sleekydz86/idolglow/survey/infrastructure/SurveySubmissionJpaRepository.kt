package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import org.springframework.data.jpa.repository.JpaRepository

interface SurveySubmissionJpaRepository : JpaRepository<SurveySubmission, Long> {
    fun findFirstByUserIdOrderByIdDesc(userId: Long): SurveySubmission?
    fun findByIdAndUserId(id: Long, userId: Long): SurveySubmission?
}
