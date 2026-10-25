package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleFestivalPort
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTextLookup
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EventTranslationFallbackTest {
    @Test
    fun japaneseRequestWithoutJapaneseUsesEnglish() {
        val translations = InMemoryEventTranslationPort()
        translations.save(EventTranslation.provider("g-1", "en", "English title", "English synopsis"))
        translations.save(EventTranslation.provider("g-1", "ko", "한국어 제목", "한국어 소개"))
        val refs = InMemoryEventSourceRefPort()
        refs.save(sourceRef("TOUR_API", "C1", "g-1"))
        val masters = InMemoryEventMasterPort()
        masters.save(master("g-1", "한국어 제목"))
        val service = EventLocalizedTextService(translations, refs, masters)

        val resolved =
            service
                .resolve(
                    listOf(EventTextLookup("TOUR_API", "C1", "한국어 제목", "한국어 소개")),
                    "ja",
                ).single()
        val graph = service.title("g-1", "ja")

        assertEquals("English title", resolved.title)
        assertEquals("English synopsis", resolved.synopsis)
        assertEquals("en", resolved.locale)
        assertEquals("en", graph?.locale)
        assertEquals("English title", graph?.title)
    }

    @Test
    fun machineOriginIsNotServed() {
        val translations = InMemoryEventTranslationPort()
        translations.save(
            EventTranslation(
                globalId = "g-1",
                locale = "ja",
                title = "機械翻訳",
                synopsis = null,
                origin = EventTranslationOrigin.MACHINE,
            ),
        )
        translations.save(EventTranslation.provider("g-1", "en", "English title", null))
        val masters = InMemoryEventMasterPort()
        masters.save(master("g-1", "한국어 제목"))
        val service = EventLocalizedTextService(translations, InMemoryEventSourceRefPort(), masters)

        val resolved = service.title("g-1", "ja")

        assertEquals("en", resolved?.locale)
        assertEquals("English title", resolved?.title)
    }

    @Test
    fun providerTranslationChangeReplacesStoredText() {
        val translations = InMemoryEventTranslationPort()
        val tx = EventTranslationTxService(translations)

        tx.upsertProvider("g-1", "en", "Old title", "old synopsis")
        tx.upsertProvider("g-1", "en", "New title", "new synopsis")
        val saved = translations.findByGlobalIdAndLocale("g-1", "en")

        assertEquals("New title", saved?.title)
        assertEquals("new synopsis", saved?.synopsis)
        assertEquals(EventTranslationOrigin.PROVIDER, saved?.origin)
        assertEquals(1, translations.findByGlobalId("g-1").size)
    }

    @Test
    fun manualTranslationStaysWhenProviderChanges() {
        val translations = InMemoryEventTranslationPort()
        translations.save(
            EventTranslation(
                globalId = "g-1",
                locale = "en",
                title = "운영자 제목",
                synopsis = "운영자 소개",
                origin = EventTranslationOrigin.MANUAL,
            ),
        )
        val tx = EventTranslationTxService(translations)

        tx.upsertProvider("g-1", "en", "Provider title", "provider synopsis")
        val saved = translations.findByGlobalIdAndLocale("g-1", "en")

        assertEquals("운영자 제목", saved?.title)
        assertEquals("운영자 소개", saved?.synopsis)
        assertEquals(EventTranslationOrigin.MANUAL, saved?.origin)
    }

    @Test
    fun tourContentIdLinksTranslationToGlobalId() {
        val translations = InMemoryEventTranslationPort()
        val refs = InMemoryEventSourceRefPort()
        refs.save(sourceRef("TOUR_API", "99", "g-1"))
        refs.save(sourceRef("TOUR_API", "100", null))
        val tour =
            FakeTourLocaleFestivalPort(
                mapOf(
                    "ja" to
                        listOf(
                            festival("99", "사쿠라 축제"),
                            festival("100", "연결 없는 공연"),
                            festival("101", "미매칭 공연"),
                        ),
                ),
            )
        val collector =
            TourEventTranslationCollector(
                tourLocaleFestivalPort = tour,
                eventSourceRefPort = refs,
                eventTranslationTxService = EventTranslationTxService(translations),
            )

        collector.collect(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        val saved = translations.findByGlobalId("g-1")

        assertEquals(listOf("ja"), saved.map { it.locale })
        assertEquals("사쿠라 축제", saved.single().title)
        assertEquals(EventTranslationOrigin.PROVIDER, saved.single().origin)
        assertNull(translations.findByGlobalIdAndLocale("g-missing", "ja"))
    }

    @Test
    fun queryLangOverridesAcceptLanguage() {
        assertEquals("ja", EventLocale.requested("ja-JP", "en-US,en;q=0.9"))
        assertEquals("en", EventLocale.requested(null, "en-US,en;q=0.9"))
        assertEquals("ko", EventLocale.requested(" ", null))
    }
}

private class InMemoryEventTranslationPort : EventTranslationPort {
    private val rows = linkedMapOf<Pair<String, String>, EventTranslation>()

    override fun save(translation: EventTranslation): EventTranslation {
        rows[translation.globalId to translation.locale] = translation
        return translation
    }

    override fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation? = rows[globalId to locale]

    override fun findByGlobalId(globalId: String): List<EventTranslation> =
        rows.values.filter { it.globalId == globalId }

    override fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation> =
        rows.values.filter { it.globalId in globalIds }
}

private class InMemoryEventSourceRefPort : EventSourceRefPort {
    private val rows = mutableListOf<EventSourceRef>()

    override fun save(ref: EventSourceRef): EventSourceRef {
        rows.removeAll { it.source == ref.source && it.sourceId == ref.sourceId }
        rows += ref
        return ref
    }

    override fun findBySourceAndSourceId(
        source: String,
        sourceId: String,
    ): EventSourceRef? = rows.find { it.source == source && it.sourceId == sourceId }

    override fun findBySourceInAndSourceIdIn(
        sources: Collection<String>,
        sourceIds: Collection<String>,
    ): List<EventSourceRef> = rows.filter { it.source in sources && it.sourceId in sourceIds }

    override fun countByGlobalId(globalId: String): Long = rows.count { it.globalId == globalId }.toLong()

    override fun countByMatchStatus(status: EventMatchStatus): Long = rows.count { it.matchStatus == status }.toLong()

    override fun findAll(): List<EventSourceRef> = rows.toList()
}

private class InMemoryEventMasterPort : EventMasterPort {
    private val rows = linkedMapOf<String, EventMaster>()

    override fun save(master: EventMaster): EventMaster {
        rows[master.globalId] = master
        return master
    }

    override fun findByGlobalId(globalId: String): EventMaster? = rows[globalId]

    override fun findByNormalizedTitle(normalizedTitle: String): List<EventMaster> =
        rows.values.filter { it.normalizedTitle == normalizedTitle }

    override fun count(): Long = rows.size.toLong()
}

private class FakeTourLocaleFestivalPort(
    private val byLocale: Map<String, List<FestivalEvent>>,
) : TourLocaleFestivalPort {
    override fun searchFestival(
        locale: String,
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent> = if (pageNo == 1) byLocale[locale].orEmpty() else emptyList()
}

private fun sourceRef(
    source: String,
    sourceId: String,
    globalId: String?,
): EventSourceRef =
    EventSourceRef(
        source = source,
        sourceId = sourceId,
        globalId = globalId,
        matchStatus =
            if (globalId == null) {
                EventMatchStatus.CONFLICT
            } else {
                EventMatchStatus.CONFIRMED
            },
        matchScore = 0,
        rawHash = "hash",
        title = sourceId,
    )

private fun master(
    globalId: String,
    title: String,
): EventMaster =
    EventMaster(
        globalId = globalId,
        title = title,
        normalizedTitle = title,
        startDate = null,
        endDate = null,
        venue = null,
        normalizedVenue = null,
        status = EventMasterStatus.ACTIVE,
    )

private fun festival(
    contentId: String,
    title: String,
): FestivalEvent =
    FestivalEvent(
        contentId = contentId,
        title = title,
        address = null,
        eventStartDate = null,
        eventEndDate = null,
        thumbnailImageUrl = null,
        imageUrl = null,
        mapX = null,
        mapY = null,
        phone = null,
    )
