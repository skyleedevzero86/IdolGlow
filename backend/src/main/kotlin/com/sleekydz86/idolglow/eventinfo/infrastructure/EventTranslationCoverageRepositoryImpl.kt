package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationCoveragePort
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.MissingTranslation
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class EventTranslationCoverageRepositoryImpl(
    private val repository: EventTranslationCoverageJpaRepository,
) : EventTranslationCoveragePort {
    override fun countActive(): Long = repository.countByStatus(EventMasterStatus.ACTIVE)

    override fun countVisible(locale: String): Long =
        repository.countWithOrigin(EventMasterStatus.ACTIVE, locale, EventTranslationOrigin.VISIBLE)

    override fun countMachineOnly(locale: String): Long =
        repository.countHiddenOnly(
            EventMasterStatus.ACTIVE,
            locale,
            EventTranslationOrigin.HIDDEN,
            EventTranslationOrigin.VISIBLE,
        )

    override fun countMissing(locale: String): Long = repository.countWithoutLocale(EventMasterStatus.ACTIVE, locale)

    override fun findMissing(
        locale: String,
        limit: Int,
    ): List<MissingTranslation> {
        if (limit <= 0) return emptyList()
        return repository.findWithoutLocale(EventMasterStatus.ACTIVE, locale, PageRequest.of(0, limit))
    }
}
