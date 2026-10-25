package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class EventTranslationTxService(
    private val eventTranslationPort: EventTranslationPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertProvider(
        globalId: String,
        locale: String,
        title: String,
        synopsis: String?,
    ) {
        val normalizedTitle = title.trim().take(EventTranslation.TITLE_MAX)
        val normalizedSynopsis = synopsis?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedLocale = EventLocale.normalize(locale)
        val existing = eventTranslationPort.findByGlobalIdAndLocale(globalId, normalizedLocale)
        if (blocked(existing, normalizedTitle, normalizedSynopsis)) return
        val row = existing ?: EventTranslation.provider(globalId, normalizedLocale, normalizedTitle, normalizedSynopsis)
        existing?.replaceProviderText(normalizedTitle, normalizedSynopsis)
        eventTranslationPort.save(row)
    }

    private fun blocked(
        existing: EventTranslation?,
        title: String,
        synopsis: String?,
    ): Boolean =
        when {
            title.isEmpty() -> true
            existing == null -> false
            existing.origin != EventTranslationOrigin.PROVIDER -> true
            else -> existing.title == title && existing.synopsis == synopsis
        }
}
