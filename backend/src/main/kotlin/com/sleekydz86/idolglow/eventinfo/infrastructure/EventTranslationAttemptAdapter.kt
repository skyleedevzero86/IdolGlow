package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationAttemptPort
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt
import org.springframework.stereotype.Repository

@Repository
class EventTranslationAttemptAdapter(
    private val eventTranslationAttemptJpaRepository: EventTranslationAttemptJpaRepository,
) : EventTranslationAttemptPort {
    override fun find(
        globalId: String,
        locale: String,
    ): EventTranslationAttempt? = eventTranslationAttemptJpaRepository.findByGlobalIdAndLocale(globalId, locale)

    override fun save(attempt: EventTranslationAttempt): EventTranslationAttempt =
        eventTranslationAttemptJpaRepository.saveAndFlush(attempt)

    override fun delete(
        globalId: String,
        locale: String,
    ) {
        eventTranslationAttemptJpaRepository.deleteByGlobalIdAndLocale(globalId, locale)
    }
}
