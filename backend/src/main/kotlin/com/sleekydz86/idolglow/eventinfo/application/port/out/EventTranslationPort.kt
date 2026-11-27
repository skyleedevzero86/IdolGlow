package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import java.time.LocalDate

interface EventTranslationPort {
    fun save(translation: EventTranslation): EventTranslation

    fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation?

    fun findByGlobalId(globalId: String): List<EventTranslation>

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation>

    fun findUntranslatedFestivals(
        source: String,
        locale: String,
        from: LocalDate,
        to: LocalDate,
        limit: Int,
    ): List<EventTitleTarget>
}
