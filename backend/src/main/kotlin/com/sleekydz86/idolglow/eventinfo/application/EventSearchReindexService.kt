package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventSearchExceptionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventSearchReindexService(
    private val eventMasterPort: EventMasterPort,
    private val eventTranslationPort: EventTranslationPort,
    private val eventSearchIndexPort: EventSearchIndexPort,
    private val properties: EventSearchProperties,
) {
    private val log = LoggerFactory.getLogger(EventSearchReindexService::class.java)

    fun reindexAll(): EventSearchReindexResult {
        if (!properties.enabled) throw CustomException(EventSearchExceptionType.DISABLED)
        val until = LocalDateTime.now()
        var cursor = START
        var lastGlobalId = ""
        var result = EventSearchReindexResult(0, 0)
        var page = eventMasterPort.findChangedAfter(cursor, lastGlobalId, until, PAGE_SIZE)
        while (page.isNotEmpty()) {
            result += indexPage(page)
            cursor = checkNotNull(page.last().updatedAt) { "공연 수정 시각이 비어 있습니다. globalId=${page.last().globalId}" }
            lastGlobalId = page.last().globalId
            page = eventMasterPort.findChangedAfter(cursor, lastGlobalId, until, PAGE_SIZE)
        }
        log.info("공연 검색 색인 다시 만들기 완료. indexed={} stale={}", result.indexed, result.stale)
        return result
    }

    private fun indexPage(page: List<EventMaster>): EventSearchReindexResult {
        val translations = eventTranslationPort.findByGlobalIdIn(page.map { it.globalId }).groupBy { it.globalId }
        val outcomes =
            page.map { master ->
                val document =
                    EventSearchDocument.of(
                        master.globalId,
                        master.snapshot(),
                        master.metaVersion,
                        translations[master.globalId].orEmpty(),
                    )
                eventSearchIndexPort.index(document, overwriteSameVersion = true)
            }
        return EventSearchReindexResult(
            indexed = outcomes.count { it == EventSearchIndexOutcome.INDEXED },
            stale = outcomes.count { it == EventSearchIndexOutcome.STALE },
        )
    }

    companion object {
        private const val PAGE_SIZE: Int = 200
        private val START: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)
    }
}

data class EventSearchReindexResult(
    val indexed: Int,
    val stale: Int,
) {
    operator fun plus(other: EventSearchReindexResult): EventSearchReindexResult =
        EventSearchReindexResult(indexed + other.indexed, stale + other.stale)
}
