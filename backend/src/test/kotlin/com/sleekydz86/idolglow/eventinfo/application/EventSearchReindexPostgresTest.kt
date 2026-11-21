package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventSearchExceptionType
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@DisplayName("공연 검색 색인 다시 만들기")
class EventSearchReindexPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventMasterPort: EventMasterPort

    @Autowired private lateinit var eventTranslationPort: EventTranslationPort

    @Autowired private lateinit var eventTranslationTxService: EventTranslationTxService

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private val index = RecordingIndex()

    @BeforeEach
    fun clean() {
        eventTranslationJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("대표 공연 전부를 지금 버전과 번역 제목으로, 같은 버전 덮어쓰기 방식으로 넣는다")
    @Test
    fun reindexesEveryMasterWithCurrentVersion() {
        // given
        공연("gid-a", metaVersion = 3)
        공연("gid-b", metaVersion = 1)
        공연("gid-c", metaVersion = 7, status = EventMasterStatus.MERGED)
        eventTranslationTxService.upsertProvider("gid-a", "en", "Seoul Concert", null)
        index.stale += "gid-b"

        // when
        val result = 서비스(enabled = true).reindexAll()

        // then
        assertEquals(EventSearchReindexResult(indexed = 2, stale = 1), result)
        assertEquals(setOf("gid-a", "gid-b", "gid-c"), index.documents.map { it.globalId }.toSet())
        assertEquals(setOf(true), index.overwrites.toSet())
        val gidA = index.documents.single { it.globalId == "gid-a" }
        assertEquals(mapOf("en" to "Seoul Concert"), gidA.titles)
        assertEquals("MERGED", index.documents.single { it.globalId == "gid-c" }.status)
    }

    @DisplayName("검색이 꺼져 있으면 다시 만들지 않고 503으로 알린다")
    @Test
    fun disabledSearchIsRejected() {
        // when
        val error = assertThrows<CustomException> { 서비스(enabled = false).reindexAll() }

        // then
        assertEquals(EventSearchExceptionType.DISABLED, error.getExceptionType())
        assertEquals(0, index.documents.size)
    }

    private fun 서비스(enabled: Boolean): EventSearchReindexService =
        EventSearchReindexService(
            eventMasterPort,
            eventTranslationPort,
            index,
            EventSearchProperties(enabled = enabled),
        )

    private fun 공연(
        globalId: String,
        metaVersion: Long,
        status: EventMasterStatus = EventMasterStatus.ACTIVE,
    ) {
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "검색 공연 $globalId",
                normalizedTitle = "검색공연" + globalId.replace("-", ""),
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 31),
                venue = "세종문화회관",
                normalizedVenue = "세종문화회관",
                status = status,
            ),
        )
        jdbcTemplate.update("update event_master set meta_version = ? where global_id = ?", metaVersion, globalId)
    }

    private class RecordingIndex : EventSearchIndexPort {
        val documents: MutableList<EventSearchDocument> = mutableListOf()
        val overwrites: MutableList<Boolean> = mutableListOf()
        val stale: MutableSet<String> = mutableSetOf()

        override fun index(
            document: EventSearchDocument,
            overwriteSameVersion: Boolean,
        ): EventSearchIndexOutcome {
            documents += document
            overwrites += overwriteSameVersion
            return if (document.globalId in stale) EventSearchIndexOutcome.STALE else EventSearchIndexOutcome.INDEXED
        }

        override fun search(
            query: String,
            size: Int,
        ): List<EventSearchHit> = emptyList()
    }
}
