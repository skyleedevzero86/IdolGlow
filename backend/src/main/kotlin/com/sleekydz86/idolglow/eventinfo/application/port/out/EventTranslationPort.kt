package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation

interface EventTranslationPort {
    fun save(translation: EventTranslation): EventTranslation

    fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation?

    fun findByGlobalId(globalId: String): List<EventTranslation>

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation>
}
