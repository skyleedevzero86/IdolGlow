package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventTranslationAttemptJpaRepository : JpaRepository<EventTranslationAttempt, Long> {
    fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslationAttempt?

    @Modifying
    @Query("delete from EventTranslationAttempt a where a.globalId = :globalId and a.locale = :locale")
    fun deleteByGlobalIdAndLocale(
        @Param("globalId") globalId: String,
        @Param("locale") locale: String,
    ): Int
}
