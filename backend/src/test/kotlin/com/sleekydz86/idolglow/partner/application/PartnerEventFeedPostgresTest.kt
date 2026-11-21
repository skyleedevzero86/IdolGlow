package com.sleekydz86.idolglow.partner.application

import com.sleekydz86.idolglow.eventinfo.application.EventExternalIdService
import com.sleekydz86.idolglow.eventinfo.application.EventMasterMergeHandler
import com.sleekydz86.idolglow.eventinfo.application.EventTranslationTxService
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventExternalIdJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventExternalIdExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.partner.PartnerExceptionType
import com.sleekydz86.idolglow.partner.application.dto.PartnerEventPage
import com.sleekydz86.idolglow.partner.application.dto.PartnerExternalId
import com.sleekydz86.idolglow.partner.application.dto.PartnerFeedQuery
import com.sleekydz86.idolglow.partner.infrastructure.PartnerCallLogJpaRepository
import com.sleekydz86.idolglow.partner.infrastructure.PartnerClientJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("파트너 변경분 피드와 글로벌 공연 병합 (Postgres)")
class PartnerEventFeedPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var partnerEventFeedService: PartnerEventFeedService

    @Autowired private lateinit var partnerAdminService: PartnerAdminService

    @Autowired private lateinit var eventMasterMergeHandler: EventMasterMergeHandler

    @Autowired private lateinit var eventTranslationTxService: EventTranslationTxService

    @Autowired private lateinit var eventMasterPort: EventMasterPort

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @Autowired private lateinit var partnerClientJpaRepository: PartnerClientJpaRepository

    @Autowired private lateinit var partnerCallLogJpaRepository: PartnerCallLogJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @Autowired private lateinit var eventExternalIdService: EventExternalIdService

    @Autowired private lateinit var eventExternalIdJpaRepository: EventExternalIdJpaRepository

    @BeforeEach
    fun clean() {
        partnerCallLogJpaRepository.deleteAllInBatch()
        partnerClientJpaRepository.deleteAllInBatch()
        eventTranslationJpaRepository.deleteAllInBatch()
        eventExternalIdJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventCollectLogJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("P4 커서를 끝까지 따라 받은 결과가 전체 조회와 건수·ID·순서가 같다")
    @Test
    fun cursorWalkMatchesFullScan() {
        // given
        val ids = (0 until 10).map { 공연("gid-%02d".format(it), BASE.plusMinutes(it.toLong())) }
        val key = 파트너_키()

        // when
        val walked = 끝까지(key, size = 3)

        // then
        assertEquals(ids, walked)
        assertEquals(eventMasterJpaRepository.count().toInt(), walked.size)
        assertEquals(listOf(200, 200, 200, 200), partnerCallLogJpaRepository.findAll().map { it.status })
        assertEquals(10, partnerCallLogJpaRepository.findAll().sumOf { it.itemCount })
    }

    @DisplayName("P5 받는 도중 수정된 공연은 빠지지 않고 뒤에서 다시 온다")
    @Test
    fun changesDuringWalkAreNotLost() {
        // given
        (0 until 10).forEach { 공연("gid-%02d".format(it), BASE.plusMinutes(it.toLong())) }
        val key = 파트너_키()

        // when
        val walked =
            끝까지(key, size = 3) {
                수정_시각("gid-01", BASE.plusHours(1))
                수정_시각("gid-05", BASE.plusHours(1).plusMinutes(1))
            }

        // then
        assertEquals(
            listOf("00", "01", "02", "03", "04", "06", "07", "08", "09", "01", "05").map { "gid-$it" },
            walked,
        )
    }

    @DisplayName("P6 병합된 옛 글로벌 ID를 요청하면 이어받은 글로벌 ID를 알려 주고, 되돌리면 원래대로 돌아온다")
    @Test
    fun mergedMasterPointsToSurvivor() {
        // given
        공연("gid-old", BASE)
        공연("gid-new", BASE.plusMinutes(1))
        원본("KOPIS_PBLPRFR", "PF-OLD", "gid-old")
        원본("SEOUL_SJW_PERFORM", "SJW-NEW", "gid-new")
        val key = 파트너_키()
        val payload = """{"intoGlobalId":"gid-new"}"""
        eventMasterMergeHandler.validate("gid-old", payload)
        val snapshot = eventMasterMergeHandler.snapshot("gid-old")

        // when
        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.apply("gid-old", payload) }
        val merged = partnerEventFeedService.event(key, "gid-old", "en")

        // then
        assertEquals("MERGED", merged.status)
        assertEquals("gid-new", merged.mergedInto)
        assertEquals("gid-new", 원본_글로벌_ID("KOPIS_PBLPRFR", "PF-OLD"))
        assertFalse(eventMasterPort.findActiveByNormalizedTitle("공연gidold").any { it.globalId == "gid-old" })

        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.revert("gid-old", snapshot) }
        val restored = checkNotNull(eventMasterJpaRepository.findByGlobalId("gid-old"))
        assertEquals(EventMasterStatus.ACTIVE, restored.status)
        assertNull(restored.mergedInto)
        assertEquals("gid-old", 원본_글로벌_ID("KOPIS_PBLPRFR", "PF-OLD"))
    }

    @DisplayName("이미 받아 간 공연에 번역이 새로 붙으면 다음 변경분에 다시 나온다")
    @Test
    fun newTranslationAppearsInNextFeed() {
        // given
        공연("gid-a", BASE)
        val key = 파트너_키()
        val first = partnerEventFeedService.feed(key, PartnerFeedQuery(SINCE, null, 10, "en"))
        eventTranslationTxService.upsertProvider("gid-a", "en", "Seoul Concert", null)

        // when
        val next =
            try {
                partnerEventFeedService.clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(10))
                partnerEventFeedService.feed(key, PartnerFeedQuery(null, first.nextCursor, 10, "en"))
            } finally {
                partnerEventFeedService.clock = Clock.systemDefaultZone()
            }

        // then
        assertEquals(listOf("ko" to "공연 gid-a"), first.items.map { it.locale to it.title })
        assertEquals(listOf("en" to "Seoul Concert"), next.items.map { it.locale to it.title })
    }

    @DisplayName("계약 언어 번역이 있으면 그 언어로, 없으면 한국어 원문으로 준다")
    @Test
    fun servesContractedTranslationOrKorean() {
        // given
        공연("gid-en", BASE)
        공연("gid-ko", BASE.plusMinutes(1))
        eventTranslationJpaRepository.save(
            EventTranslation(
                globalId = "gid-en",
                locale = "en",
                title = "Seoul Concert",
                origin = EventTranslationOrigin.PROVIDER,
            ),
        )
        val key = 파트너_키()

        // when
        val page = partnerEventFeedService.feed(key, PartnerFeedQuery(SINCE, null, 10, "en"))

        // then
        assertEquals(listOf("en" to "Seoul Concert", "ko" to "공연 gid-ko"), page.items.map { it.locale to it.title })
    }

    private fun 끝까지(
        key: String,
        size: Int,
        afterFirstPage: () -> Unit = {},
    ): List<String> {
        val ids = mutableListOf<String>()
        var cursor: String? = null
        var page: PartnerEventPage
        var first = true
        do {
            page = partnerEventFeedService.feed(key, PartnerFeedQuery(SINCE, cursor, size, "en"))
            ids += page.items.map { it.globalId }
            cursor = page.nextCursor
            if (first) afterFirstPage()
            first = false
        } while (page.hasMore)
        return ids
    }

    @DisplayName("KOPIS 번호와 위키데이터 번호로 같은 공연을 찾고, 응답에 확정된 외부 번호만 담긴다")
    @Test
    fun findsEventByExternalIds() {
        // given
        공연("gid-a", BASE)
        원본("KOPIS_PBLPRFR", "PF-A", "gid-a")
        후보_원본("TOUR_API", "KTO-A", "gid-a")
        eventExternalIdService.attach("gid-a", ExternalIdScheme.WIKIDATA, "Q12345")
        val key = 파트너_키()

        // when
        val byKopis = partnerEventFeedService.eventByExternalId(key, "KOPIS", "PF-A", "en")
        val byWikidata = partnerEventFeedService.eventByExternalId(key, "wikidata", "Q12345", "en")
        val missing =
            assertThrows<CustomException> {
                partnerEventFeedService.eventByExternalId(key, "KOPIS", "PF-NONE", "en")
            }

        // then
        val expected = listOf(PartnerExternalId("KOPIS", "PF-A"), PartnerExternalId("WIKIDATA", "Q12345"))
        assertEquals("gid-a" to expected, byKopis.globalId to byKopis.externalIds)
        assertEquals("gid-a", byWikidata.globalId)
        assertEquals(PartnerExceptionType.EVENT_NOT_FOUND, missing.getExceptionType())
    }

    @DisplayName("위키데이터 번호는 다른 공연에 다시 붙일 수 없고, 형식이 틀리거나 공급처 번호면 거절한다")
    @Test
    fun rejectsInvalidExternalIds() {
        // given
        공연("gid-a", BASE)
        공연("gid-b", BASE)
        eventExternalIdService.attach("gid-a", ExternalIdScheme.WIKIDATA, "Q12345")

        // when
        val duplicated =
            assertThrows<CustomException> {
                eventExternalIdService.attach("gid-b", ExternalIdScheme.WIKIDATA, "Q12345")
            }
        val badFormat =
            assertThrows<IllegalArgumentException> {
                eventExternalIdService.attach("gid-b", ExternalIdScheme.WIKIDATA, "12345")
            }
        val sourceScheme =
            assertThrows<IllegalArgumentException> {
                eventExternalIdService.attach("gid-b", ExternalIdScheme.KOPIS, "PF-B")
            }

        // then
        assertEquals(EventExternalIdExceptionType.ALREADY_LINKED, duplicated.getExceptionType())
        assertEquals("외부 ID 형식이 올바르지 않습니다. scheme=WIKIDATA externalId=12345", badFormat.message)
        assertEquals("출처에서 자동으로 오는 외부 ID는 직접 붙일 수 없습니다. scheme=KOPIS", sourceScheme.message)
    }

    @DisplayName("공연을 합치면 위키데이터 번호도 남는 공연으로 옮겨지고, 되돌리면 원래 공연으로 돌아온다")
    @Test
    fun mergeMovesWikidataIdAndRevertRestoresIt() {
        // given
        공연("gid-old", BASE)
        공연("gid-new", BASE)
        eventExternalIdService.attach("gid-old", ExternalIdScheme.WIKIDATA, "Q777")
        val payload = """{"intoGlobalId":"gid-new"}"""
        val snapshot = eventMasterMergeHandler.snapshot("gid-old")

        // when
        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.apply("gid-old", payload) }
        val afterMerge = eventExternalIdService.resolve(ExternalIdScheme.WIKIDATA, "Q777")
        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.revert("gid-old", snapshot) }
        val afterRevert = eventExternalIdService.resolve(ExternalIdScheme.WIKIDATA, "Q777")

        // then
        assertEquals("gid-new" to "gid-old", afterMerge to afterRevert)
    }

    @DisplayName("이미 받아 간 공연에 위키데이터 번호를 붙이면 다음 변경분에 다시 나온다")
    @Test
    fun newExternalIdAppearsInNextFeed() {
        // given
        공연("gid-a", BASE)
        val key = 파트너_키()
        val first = partnerEventFeedService.feed(key, PartnerFeedQuery(SINCE, null, 10, "en"))
        eventExternalIdService.attach("gid-a", ExternalIdScheme.WIKIDATA, "Q999")

        // when
        val next =
            try {
                partnerEventFeedService.clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(10))
                partnerEventFeedService.feed(key, PartnerFeedQuery(null, first.nextCursor, 10, "en"))
            } finally {
                partnerEventFeedService.clock = Clock.systemDefaultZone()
            }

        // then
        assertEquals(emptyList<PartnerExternalId>(), first.items.single().externalIds)
        assertEquals(listOf(PartnerExternalId("WIKIDATA", "Q999")), next.items.single().externalIds)
    }

    private fun 후보_원본(
        source: String,
        sourceId: String,
        globalId: String,
    ) {
        eventSourceRefJpaRepository.save(
            EventSourceRef(
                source = source,
                sourceId = sourceId,
                globalId = globalId,
                matchStatus = EventMatchStatus.CANDIDATE,
                matchScore = 60,
                rawHash = "hash-$sourceId",
                title = "공연 $sourceId",
            ),
        )
    }

    private fun 파트너_키(): String = partnerAdminService.issue("서울 여행사", listOf("en"), 1000).apiKey

    private fun 공연(
        globalId: String,
        updatedAt: LocalDateTime,
    ): String {
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "공연 $globalId",
                normalizedTitle = "공연" + globalId.replace("-", ""),
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 3),
                venue = "세종문화회관",
                normalizedVenue = "세종문화회관",
                status = EventMasterStatus.ACTIVE,
            ),
        )
        수정_시각(globalId, updatedAt)
        return globalId
    }

    private fun 원본(
        source: String,
        sourceId: String,
        globalId: String,
    ) {
        eventSourceRefJpaRepository.save(
            EventSourceRef(
                source = source,
                sourceId = sourceId,
                globalId = globalId,
                matchStatus = EventMatchStatus.CONFIRMED,
                matchScore = 100,
                rawHash = "hash-$sourceId",
                title = "공연 $sourceId",
            ),
        )
    }

    private fun 수정_시각(
        globalId: String,
        updatedAt: LocalDateTime,
    ) {
        jdbcTemplate.update(
            "update event_master set updated_at = ? where global_id = ?",
            Timestamp.valueOf(updatedAt),
            globalId,
        )
    }

    private fun 원본_글로벌_ID(
        source: String,
        sourceId: String,
    ): String? = eventSourceRefJpaRepository.findBySourceAndSourceId(source, sourceId)?.globalId

    companion object {
        private val BASE: LocalDateTime = LocalDateTime.of(2020, 1, 1, 0, 0)
        private const val SINCE: String = "2020-01-01T00:00:00"
    }
}
