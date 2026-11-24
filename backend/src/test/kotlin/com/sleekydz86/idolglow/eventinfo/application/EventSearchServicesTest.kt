package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventSearchExceptionType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("공연 검색 색인 반영과 검색 조회")
class EventSearchServicesTest {
    private val indexPort: EventSearchIndexPort = mockk()
    private val translationPort: EventTranslationPort = mockk()
    private val sourceRefPort: EventSourceRefPort = mockk()

    @DisplayName("메타 변경 이벤트를 받으면 한국어를 뺀 번역 제목을 함께 색인하고, 옛 버전이면 STALE로 끝낸다")
    @Test
    fun indexesWithTranslatedTitles() {
        // given
        val captured = slot<EventSearchDocument>()
        every { translationPort.findByGlobalId("gid-a") } returns
            listOf(번역("en", "Seoul Concert"), 번역("ko", "서울 공연"))
        every { indexPort.index(capture(captured), false) } returnsMany
            listOf(EventSearchIndexOutcome.INDEXED, EventSearchIndexOutcome.STALE)
        val service = EventSearchIndexService(indexPort, translationPort)

        // when
        val first = service.handle(메시지(version = 4))
        val second = service.handle(메시지(version = 3))
        val noSnapshot = service.handle(메시지(version = 5).copy(snapshot = null))

        // then
        assertEquals(
            listOf(EventMetaApplyResult.APPLIED, EventMetaApplyResult.STALE, EventMetaApplyResult.IGNORED),
            listOf(first, second, noSnapshot),
        )
        assertEquals(mapOf("en" to "Seoul Concert"), captured.captured.titles)
        assertEquals(3L, captured.captured.metaVersion)
    }

    @DisplayName("요청 언어의 번역 제목을 주고, 확정 연결된 원본 중 먼저 생긴 것으로 상세 주소를 만든다")
    @Test
    fun searchReturnsLocalizedTitleAndDetailLink() {
        // given
        every { indexPort.search("concert", 20) } returns listOf(결과("gid-a"))
        every { sourceRefPort.findByGlobalIdIn(listOf("gid-a")) } returns
            listOf(
                원본(2, "TOUR_API", "T-1", EventMatchStatus.CONFIRMED),
                원본(1, "KOPIS_PBLPRFR", "PF-1", EventMatchStatus.CONFIRMED),
            )

        // when
        val items = 검색(enabled = true).search(" concert ", "en", null)

        // then
        assertEquals(
            listOf(Triple("Seoul Concert", "KOPIS_PBLPRFR", "PF-1")),
            items.map { Triple(it.title, it.source, it.sourceId) },
        )
    }

    @DisplayName("검색이 꺼져 있거나 검색 엔진이 응답하지 않으면 503, 빈 검색어는 400으로 알린다")
    @Test
    fun searchFailures() {
        // given
        every { indexPort.search(any(), any()) } throws IllegalStateException("검색 엔진 연결 실패. path=/x")

        // when
        val disabled = assertThrows<CustomException> { 검색(enabled = false).search("공연", null, null) }
        val failed = assertThrows<CustomException> { 검색(enabled = true).search("공연", null, null) }
        val blank = assertThrows<IllegalArgumentException> { 검색(enabled = true).search("  ", null, null) }

        // then
        assertEquals(EventSearchExceptionType.DISABLED, disabled.getExceptionType())
        assertEquals(EventSearchExceptionType.FAILED, failed.getExceptionType())
        assertEquals("검색어를 입력해 주세요.", blank.message)
    }

    private fun 검색(enabled: Boolean): EventSearchQueryService =
        EventSearchQueryService(indexPort, sourceRefPort, EventSearchProperties(enabled = enabled))

    private fun 메시지(version: Long): EventMetaMessage =
        EventMetaMessage(
            eventId = "evt-$version",
            eventType = "UPDATED",
            globalId = "gid-a",
            metaVersion = version,
            snapshot = EventMetaSnapshot(title = "서울 공연", status = "ACTIVE"),
        )

    private fun 번역(
        locale: String,
        title: String,
    ): EventTranslation = EventTranslation(globalId = "gid-a", locale = locale, title = title, origin = EventTranslationOrigin.PROVIDER)

    private fun 결과(globalId: String): EventSearchHit =
        EventSearchHit(globalId, "서울 공연", mapOf("en" to "Seoul Concert"), "세종문화회관", null, null)

    private fun 원본(
        id: Long,
        source: String,
        sourceId: String,
        status: EventMatchStatus,
    ): EventSourceRef =
        EventSourceRef(
            id = id,
            source = source,
            sourceId = sourceId,
            globalId = "gid-a",
            matchStatus = status,
            matchScore = 100,
            rawHash = "hash",
            title = "서울 공연",
        )
}
