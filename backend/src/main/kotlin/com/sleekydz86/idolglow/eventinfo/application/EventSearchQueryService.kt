package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventSearchExceptionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventSearchQueryService(
    private val eventSearchIndexPort: EventSearchIndexPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val properties: EventSearchProperties,
) {
    private val log = LoggerFactory.getLogger(EventSearchQueryService::class.java)

    fun search(
        query: String,
        lang: String?,
        size: Int?,
    ): List<EventSearchItem> {
        if (!properties.enabled) throw CustomException(EventSearchExceptionType.DISABLED)
        val text = query.trim()
        require(text.isNotEmpty()) { "검색어를 입력해 주세요." }
        require(text.length <= MAX_QUERY_LENGTH) { "검색어는 ${MAX_QUERY_LENGTH}자까지 입력할 수 있습니다." }
        val hits = searchIndex(text, (size ?: properties.defaultSize).coerceIn(1, properties.maxSize))
        val links = detailLinks(hits.map { it.globalId })
        val locale = EventLocale.normalize(lang)
        return hits.map { item(it, locale, links[it.globalId]) }
    }

    private fun searchIndex(
        text: String,
        size: Int,
    ): List<EventSearchHit> =
        try {
            eventSearchIndexPort.search(text, size)
        } catch (e: IllegalStateException) {
            log.warn("공연 검색 엔진 호출 실패. message={}", e.message)
            throw CustomException(EventSearchExceptionType.FAILED)
        }

    private fun detailLinks(globalIds: List<String>): Map<String, EventSourceRef> =
        eventSourceRefPort
            .findByGlobalIdIn(globalIds)
            .filter { it.confirmedGlobalId() != null }
            .sortedBy { it.id }
            .groupBy { checkNotNull(it.globalId) }
            .mapValues { (_, refs) -> refs.first() }

    private fun item(
        hit: EventSearchHit,
        locale: String,
        link: EventSourceRef?,
    ): EventSearchItem =
        EventSearchItem(
            globalId = hit.globalId,
            title = hit.titles[locale] ?: hit.title,
            originalTitle = hit.title,
            venue = hit.venue,
            startDate = hit.startDate,
            endDate = hit.endDate,
            source = link?.source,
            sourceId = link?.sourceId,
        )

    companion object {
        private const val MAX_QUERY_LENGTH: Int = 100
    }
}

data class EventSearchItem(
    val globalId: String,
    val title: String,
    val originalTitle: String,
    val venue: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val source: String?,
    val sourceId: String?,
)
