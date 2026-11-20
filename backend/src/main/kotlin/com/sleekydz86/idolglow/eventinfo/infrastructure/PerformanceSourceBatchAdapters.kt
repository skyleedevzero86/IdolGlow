package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceSourceBatchPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedBatch
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class KopisPerformanceBatchAdapter(
    private val kopisPerformanceApiClient: KopisPerformanceApiClient,
) : PerformanceSourceBatchPort {
    override val source: String = "KOPIS_PBLPRFR"

    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> = fetchBatch(from, to).items

    override fun fetchBatch(
        from: LocalDate,
        to: LocalDate,
    ): CollectedBatch =
        collectPagesUntilFailure(source, MAX_PAGES, PAGE_SIZE) { page ->
            kopisPerformanceApiClient.listPerformancesForBatch(
                stDate = from.basic(),
                edDate = to.basic(),
                page = page,
                rows = PAGE_SIZE,
            )
        }

    companion object {
        private const val MAX_PAGES: Int = 30
        private const val PAGE_SIZE: Int = 100
    }
}

@Component
class KopisFestivalBatchAdapter(
    private val kopisPerformanceApiClient: KopisPerformanceApiClient,
) : PerformanceSourceBatchPort {
    override val source: String = "KOPIS_PRFFEST"

    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> = fetchBatch(from, to).items

    override fun fetchBatch(
        from: LocalDate,
        to: LocalDate,
    ): CollectedBatch =
        collectPagesUntilFailure(source, MAX_PAGES, PAGE_SIZE) { page ->
            kopisPerformanceApiClient.listFestivalsForBatch(
                stDate = from.basic(),
                edDate = to.basic(),
                page = page,
                rows = PAGE_SIZE,
            )
        }

    companion object {
        private const val MAX_PAGES: Int = 30
        private const val PAGE_SIZE: Int = 100
    }
}

@Component
class SeoulSjwPerformanceBatchAdapter(
    private val seoulSjwPerformApiClient: SeoulSjwPerformApiClient,
) : PerformanceSourceBatchPort {
    override val source: String = "SEOUL_SJW_PERFORM"

    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> =
        collectPages(MAX_PAGES, PAGE_SIZE) { page ->
            seoulSjwPerformApiClient.listForBatch(pageNo = page, numOfRows = PAGE_SIZE)
        }.filter { event -> event.overlaps(from, to) }
            .map { event -> event.toCollected(source) }

    companion object {
        private const val MAX_PAGES: Int = 10
        private const val PAGE_SIZE: Int = 100
    }
}

@Component
class CultureInfoPerformanceBatchAdapter(
    private val cultureInfoApiClient: CultureInfoApiClient,
) : PerformanceSourceBatchPort {
    override val source: String = "CULTURE_INFO"

    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> = fetchBatch(from, to).items

    override fun fetchBatch(
        from: LocalDate,
        to: LocalDate,
    ): CollectedBatch {
        val targetDays = days(from, to)
        val loaded =
            targetDays.map { day ->
                cultureInfoApiClient.listEventsForCalendarDayWithCompleteness(
                    day.basic(),
                    pageNo = 1,
                    numOfRows = PAGE_SIZE,
                )
            }
        val incompleteDays = loaded.count { !it.complete }
        val items =
            loaded
                .flatMap { it.events }
                .map { event -> event.toCollected(event.source.ifBlank { source }) }
                .distinctBy { item -> item.source to item.sourceId }
        check(items.isNotEmpty() || targetDays.isEmpty() || incompleteDays < targetDays.size) {
            "문화정보 API 호출이 모든 날짜에서 실패해 받은 공연이 없습니다. days=${targetDays.size}"
        }
        return CollectedBatch(items, incompleteReason(incompleteDays, targetDays.size))
    }

    private fun incompleteReason(
        incompleteDays: Int,
        totalDays: Int,
    ): String? {
        if (incompleteDays == 0) return null
        return "API 호출 실패(요청 한도 초과 등)로 ${totalDays}일 중 ${incompleteDays}일의 공연이 일부 또는 전부 빠졌습니다."
    }

    companion object {
        private const val PAGE_SIZE: Int = 100
    }
}

@Component
class TourFestivalBatchAdapter(
    private val tourFestivalApiClient: TourFestivalApiClient,
) : PerformanceSourceBatchPort {
    override val source: String = "TOUR_API"

    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> = fetchBatch(from, to).items

    override fun fetchBatch(
        from: LocalDate,
        to: LocalDate,
    ): CollectedBatch =
        collectPagesUntilFailure(source, MAX_PAGES, PAGE_SIZE) { page ->
            tourFestivalApiClient.searchFestivalInLocale(
                locale = KOREAN_LOCALE,
                eventStartDate = from.basic(),
                eventEndDate = to.basic(),
                pageNo = page,
                numOfRows = PAGE_SIZE,
            )
        }

    companion object {
        private const val MAX_PAGES: Int = 10
        private const val PAGE_SIZE: Int = 100
        private const val KOREAN_LOCALE: String = "ko"
    }
}

private fun <T> collectPages(
    maxPages: Int,
    pageSize: Int,
    load: (page: Int) -> List<T>,
): List<T> {
    val all = mutableListOf<T>()
    var page = 1
    var more = true
    while (page <= maxPages && more) {
        val rows = load(page)
        all += rows
        more = rows.size >= pageSize
        page += 1
    }
    return all
}

private fun collectPagesUntilFailure(
    sourceName: String,
    maxPages: Int,
    pageSize: Int,
    load: (page: Int) -> List<FestivalEvent>,
): CollectedBatch {
    val all = mutableListOf<FestivalEvent>()
    var page = 1
    var more = true
    var failure: Throwable? = null
    while (page <= maxPages && more && failure == null) {
        val loaded = runCatching { load(page) }
        failure = loaded.exceptionOrNull()
        val rows = loaded.getOrDefault(emptyList())
        all += rows
        more = rows.size >= pageSize
        page += 1
    }
    if (failure != null && all.isEmpty()) throw failure
    val reason = failure?.let { "${page - 1}쪽 호출이 실패해 그 뒤 공연이 빠졌습니다. ${it.message}" }
    return CollectedBatch(all.map { event -> event.toCollected(sourceName) }, reason)
}

private fun days(
    from: LocalDate,
    to: LocalDate,
): List<LocalDate> {
    val cappedEnd = from.plusDays(MAX_COLLECT_DAYS)
    val end = if (cappedEnd.isBefore(to)) cappedEnd else to
    return generateSequence(from) { day -> day.plusDays(1) }.takeWhile { day -> !day.isAfter(end) }.toList()
}

private const val MAX_COLLECT_DAYS: Long = 30

private fun LocalDate.basic(): String = format(DateTimeFormatter.BASIC_ISO_DATE)

private fun FestivalEvent.toCollected(sourceName: String): CollectedPerformance =
    CollectedPerformance(
        source = sourceName,
        sourceId = contentId,
        title = EventSourceText.decode(title),
        venue = EventSourceText.decodeOrNull(address),
        startDate = eventStartDate.toEventDate(),
        endDate = eventEndDate.toEventDate(),
        rawPayload =
            listOf(contentId, title, address.orEmpty(), eventStartDate.orEmpty(), eventEndDate.orEmpty(), source)
                .joinToString("\n"),
    )

private fun FestivalEvent.overlaps(
    from: LocalDate,
    to: LocalDate,
): Boolean {
    val start = eventStartDate.toEventDate()
    val end = eventEndDate.toEventDate()
    if (start == null || end == null) {
        return true
    }
    return !start.isAfter(to) && !from.isAfter(end)
}

private fun String?.toEventDate(): LocalDate? {
    val text = this?.trim().orEmpty()
    val parsed =
        when {
            text.length == COMPACT_DATE_LENGTH && text.all { it.isDigit() } ->
                runCatching { LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
            text.length >= ISO_DATE_LENGTH -> runCatching { LocalDate.parse(text.take(ISO_DATE_LENGTH)) }.getOrNull()
            else -> null
        }
    return parsed
}

private const val COMPACT_DATE_LENGTH: Int = 8
private const val ISO_DATE_LENGTH: Int = 10
