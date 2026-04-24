package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import org.springframework.data.jpa.repository.JpaRepository

interface SurveyFormJpaRepository : JpaRepository<SurveyForm, Long> {
    fun findFirstByActiveTrueOrderByIdDesc(): SurveyForm?
}
