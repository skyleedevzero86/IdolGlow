package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import org.springframework.data.jpa.repository.JpaRepository

interface EventTranslationJpaRepository : JpaRepository<EventTranslation, Long> {
    fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation?

    fun findByGlobalId(globalId: String): List<EventTranslation>

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation>
}
