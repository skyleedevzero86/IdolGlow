package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SurveySubmissionJpaRepository : JpaRepository<SurveySubmission, Long> {
    @Query("select count(s) from SurveySubmission s where s.form.id = :formId")
    fun countByFormId(
        @Param("formId") formId: Long,
    ): Long

    @Query("select s from SurveySubmission s where s.form.id = :formId")
    fun findAllByFormId(
        @Param("formId") formId: Long,
    ): List<SurveySubmission>

    fun findFirstByUserIdOrderByIdDesc(userId: Long): SurveySubmission?

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): SurveySubmission?
}
