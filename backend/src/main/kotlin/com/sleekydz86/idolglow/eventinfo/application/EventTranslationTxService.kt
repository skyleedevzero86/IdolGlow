package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationAttemptPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventTranslationTxService(
    private val eventTranslationPort: EventTranslationPort,
    private val eventMasterPort: EventMasterPort,
    private val eventMetaOutboxRecorder: EventMetaOutboxRecorder,
    private val eventTranslationAttemptPort: EventTranslationAttemptPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertProvider(
        globalId: String,
        locale: String,
        title: String,
        synopsis: String?,
    ) {
        if (title.isBlank()) return
        store(EventTranslation.provider(globalId, locale, title, synopsis))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertMachine(
        globalId: String,
        locale: String,
        title: String,
    ): Boolean = store(EventTranslation.machine(globalId, locale, title))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordMachineFailure(
        globalId: String,
        locale: String,
        reason: String,
    ) {
        val now = LocalDateTime.now()
        val existing = eventTranslationAttemptPort.find(globalId, locale)
        existing?.recordFailure(reason, now)
        eventTranslationAttemptPort.save(existing ?: EventTranslationAttempt.first(globalId, locale, reason, now))
    }

    @Transactional
    fun saveManual(
        globalId: String,
        locale: String,
        title: String,
    ): EventTranslation {
        eventMasterPort.findByGlobalId(globalId.trim())
            ?: throw EntityNotFoundException("대표 공연을 찾을 수 없습니다. globalId=$globalId")
        val candidate = EventTranslation.manual(globalId, locale, title)
        store(candidate)
        return checkNotNull(eventTranslationPort.findByGlobalIdAndLocale(candidate.globalId, candidate.locale))
    }

    private fun store(candidate: EventTranslation): Boolean {
        val existing = eventTranslationPort.findByGlobalIdAndLocale(candidate.globalId, candidate.locale)
        val changed = existing?.offer(candidate.origin, candidate.title, candidate.synopsis) ?: true
        if (!changed) return false
        eventTranslationPort.save(existing ?: candidate)
        eventTranslationAttemptPort.delete(candidate.globalId, candidate.locale)
        recordChange(candidate.globalId)
        return true
    }

    private fun recordChange(globalId: String) {
        eventMasterPort.findByGlobalId(globalId)?.let { master ->
            master.markTranslationChanged(LocalDateTime.now())
            eventMetaOutboxRecorder.record(
                master,
                EventMetaChangeType.TRANSLATION_CHANGED,
                setOf(EventMetaSnapshot.FIELD_TRANSLATION),
            )
        }
    }
}
