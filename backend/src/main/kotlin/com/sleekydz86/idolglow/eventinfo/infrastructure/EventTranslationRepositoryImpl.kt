package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class EventTranslationRepositoryImpl(
    private val eventTranslationJpaRepository: EventTranslationJpaRepository,
) : EventTranslationPort {
    override fun save(translation: EventTranslation): EventTranslation = eventTranslationJpaRepository.saveAndFlush(translation)

    override fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation? = eventTranslationJpaRepository.findByGlobalIdAndLocale(globalId, locale)

    override fun findByGlobalId(globalId: String): List<EventTranslation> = eventTranslationJpaRepository.findByGlobalId(globalId)

    override fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation> =
        eventTranslationJpaRepository.findByGlobalIdIn(globalIds)

    override fun findUntranslatedFestivals(
        source: String,
        locale: String,
        from: LocalDate,
        to: LocalDate,
        limit: Int,
    ): List<EventTitleTarget> =
        eventTranslationJpaRepository
            .findUntranslatedMasters(
                source = source,
                locale = locale,
                from = from,
                to = to,
                pageable = PageRequest.of(0, limit),
            )
}
