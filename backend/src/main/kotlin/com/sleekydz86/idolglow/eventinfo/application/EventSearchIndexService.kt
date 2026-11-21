package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import org.springframework.stereotype.Service

@Service
class EventSearchIndexService(
    private val eventSearchIndexPort: EventSearchIndexPort,
    private val eventTranslationPort: EventTranslationPort,
) {
    fun handle(message: EventMetaMessage): EventMetaApplyResult {
        val snapshot = message.snapshot ?: return EventMetaApplyResult.IGNORED
        val translations = eventTranslationPort.findByGlobalId(message.globalId)
        val document = EventSearchDocument.of(message.globalId, snapshot, message.metaVersion, translations)
        return when (eventSearchIndexPort.index(document, overwriteSameVersion = false)) {
            EventSearchIndexOutcome.INDEXED -> EventMetaApplyResult.APPLIED
            EventSearchIndexOutcome.STALE -> EventMetaApplyResult.STALE
        }
    }

    companion object {
        const val CONSUMER: String = "event-meta-search-indexer"
    }
}
