package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleFestivalPort
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
        LOCALES.forEach { locale ->
            val saved = runCatching { collectLocale(locale, from, to) }
            val error = saved.exceptionOrNull() ?: return@forEach
            log.warn("관광 다국어 수집에 실패했습니다. locale={}", locale, error)
        }
    }

    private fun collectLocale(
        locale: String,
        from: LocalDate,
        to: LocalDate,
    ) {
        val events = load(locale, from, to)
        var linked = 0
        events.forEach { event ->
            if (store(locale, event)) linked += 1
        }
        log.info("관광 다국어 수집. locale={} linked={} skipped={}", locale, linked, events.size - linked)
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
    ): Boolean {
        val title = event.title.trim()
        val globalId = eventSourceRefPort.findBySourceAndSourceId(TOUR_SOURCE, event.contentId)?.globalId
        if (title.isEmpty() || globalId == null) return false
        eventTranslationTxService.upsertProvider(globalId, locale, title, event.synopsis)
        return true
    }

    companion object {
        const val TOUR_SOURCE: String = "TOUR_API"
        private val LOCALES: List<String> = listOf("en", "ja", "zh")
        private val COMPACT_DATE: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private const val MAX_PAGES: Int = 10
        private const val PAGE_SIZE: Int = 100
    }
}
