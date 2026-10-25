package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import org.springframework.stereotype.Repository

@Repository
class EventTranslationRepositoryImpl(
    private val eventTranslationJpaRepository: EventTranslationJpaRepository,
) : EventTranslationPort {
    override fun save(translation: EventTranslation): EventTranslation =
        eventTranslationJpaRepository.saveAndFlush(translation)

    override fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation? = eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, locale)

    override fun findByGlobalId(globalId: String): List<EventTranslation> =
        eventTranslationJpaRepository.findByGlobalId(globalId)

    override fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation> =
        eventTranslationJpaRepository.findByGlobalIdIn(globalIds)
}
