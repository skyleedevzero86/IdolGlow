package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleCallFailedException
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleFestivalPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationLocales
import com.sleekydz86.idolglow.eventinfo.domain.FestivalCoordinateMatch
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class TourEventTranslationCollector(
    private val tourLocaleFestivalPort: TourLocaleFestivalPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventTranslationTxService: EventTranslationTxService,
) {
    private val log = LoggerFactory.getLogger(TourEventTranslationCollector::class.java)

    fun collect(
        from: LocalDate,
        to: LocalDate,
    ) {
        val korean = lazy { loadKorean(from, to) }
        EventTranslationLocales.TARGETS.forEach { locale ->
            val saved = runCatching { collectLocale(locale, from, to, korean) }
            when (val error = saved.exceptionOrNull()) {
                null -> Unit
                is TourLocaleCallFailedException ->
                    log.warn("관광 다국어 수집에 실패했습니다. locale={} reason={}", locale, error.message)
                else -> log.warn("관광 다국어 수집에 실패했습니다. locale={}", locale, error)
            }
        }
    }

    private fun collectLocale(
        locale: String,
        from: LocalDate,
        to: LocalDate,
        korean: Lazy<List<FestivalEvent>>,
    ) {
        val events = load(locale, from, to)
        val counts = events.groupingBy { store(locale, it, korean) }.eachCount()
        val byContentId = counts[LinkMethod.CONTENT_ID] ?: 0
        val byCoordinate = counts[LinkMethod.COORDINATE] ?: 0
        log.info(
            "관광 다국어 수집. locale={} linked={} byContentId={} byCoordinate={} skipped={}",
            locale,
            byContentId + byCoordinate,
            byContentId,
            byCoordinate,
            counts[LinkMethod.SKIPPED] ?: 0,
        )
    }

    private fun loadKorean(
        from: LocalDate,
        to: LocalDate,
    ): List<FestivalEvent> {
        val events =
            runCatching { load(EventLocale.KOREAN, from, to) }
                .onFailure { log.warn("관광 국문 축제 목록을 받지 못해 좌표 연결을 건너뜁니다.", it) }
                .getOrDefault(emptyList())
        if (events.isEmpty()) log.warn("관광 국문 축제 목록이 비어 있어 좌표 연결을 할 수 없습니다.")
        return events
    }

    private fun load(
        locale: String,
        from: LocalDate,
        to: LocalDate,
    ): List<FestivalEvent> {
        val all = mutableListOf<FestivalEvent>()
        var page = 1
        var more = true
        while (page <= MAX_PAGES && more) {
            val rows =
                tourLocaleFestivalPort.searchFestival(
                    locale = locale,
                    eventStartDate = from.format(COMPACT_DATE),
                    eventEndDate = to.format(COMPACT_DATE),
                    pageNo = page,
                    numOfRows = PAGE_SIZE,
                )
            all += rows
            more = rows.size >= PAGE_SIZE
            page += 1
        }
        return all
    }

    private fun store(
        locale: String,
        event: FestivalEvent,
        korean: Lazy<List<FestivalEvent>>,
    ): LinkMethod {
        val title = event.title.trim()
        val link = if (title.isEmpty()) null else resolve(event, korean)
        val globalId = link?.ref?.confirmedGlobalId()
        if (link == null || globalId == null) {
            log.debug(
                "관광 다국어 연결 실패. locale={} contentId={} reason={} start={} end={} mapX={} mapY={}",
                locale,
                event.contentId,
                skipReason(title, link),
                event.eventStartDate,
                event.eventEndDate,
                event.mapX,
                event.mapY,
            )
            return LinkMethod.SKIPPED
        }
        eventTranslationTxService.upsertProvider(globalId, locale, title, event.synopsis)
        return link.method
    }

    private fun resolve(
        event: FestivalEvent,
        korean: Lazy<List<FestivalEvent>>,
    ): Link? {
        val direct = eventSourceRefPort.findBySourceAndSourceId(TOUR_SOURCE, event.contentId)
        if (direct != null) return Link(direct, LinkMethod.CONTENT_ID)
        return FestivalCoordinateMatch.findSingle(event, korean.value)?.let { match ->
            Link(eventSourceRefPort.findBySourceAndSourceId(TOUR_SOURCE, match.contentId), LinkMethod.COORDINATE)
        }
    }

    private fun skipReason(
        title: String,
        link: Link?,
    ): String =
        when {
            title.isEmpty() -> "TITLE_EMPTY"
            link == null -> "NO_KOREAN_MATCH"
            link.ref == null -> "SOURCE_REF_MISSING"
            else -> "NOT_CONFIRMED:${link.ref.matchStatus}"
        }

    private enum class LinkMethod { CONTENT_ID, COORDINATE, SKIPPED }

    private data class Link(
        val ref: EventSourceRef?,
        val method: LinkMethod,
    )

    companion object {
        const val TOUR_SOURCE: String = "TOUR_API"
        private val COMPACT_DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private const val MAX_PAGES: Int = 10
        private const val PAGE_SIZE: Int = 100
    }
}
