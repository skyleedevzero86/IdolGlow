package com.sleekydz86.idolglow.survey.infrastructure.autopick

import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickSetting
import org.springframework.data.jpa.repository.JpaRepository

interface SurveyAutoPickSettingJpaRepository : JpaRepository<SurveyAutoPickSetting, Long> {
    fun findByUserId(userId: Long): SurveyAutoPickSetting?

    fun findAllByEnabledTrue(): List<SurveyAutoPickSetting>
}
