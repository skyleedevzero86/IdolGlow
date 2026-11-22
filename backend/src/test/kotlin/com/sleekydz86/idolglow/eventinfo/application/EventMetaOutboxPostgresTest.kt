package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventOutboxJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@DisplayName("공연 메타 변경 outbox (Postgres)")
class EventMetaOutboxPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventMetaTxService: EventMetaTxService

    @Autowired private lateinit var eventMetaOutboxRecorder: EventMetaOutboxRecorder

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @Autowired private lateinit var eventOutboxJpaRepository: EventOutboxJpaRepository

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun clean() {
        jdbcTemplate.update("delete from event_interest")
        eventOutboxJpaRepository.deleteAllInBatch()
        eventTranslationJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventCollectLogJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("T1 공연 저장 중 예외가 나면 공연과 outbox가 둘 다 남지 않는다")
    @Test
    fun rollsBackMasterAndOutboxTogether() {
        // given
        val item = 공연("PF-T1", venue = "세종문화회관")

        // when
        assertThrows<IllegalStateException> {
            transactionTemplate.executeWithoutResult {
                val master = eventMasterJpaRepository.save(EventMaster.open(item))
                eventMetaOutboxRecorder.record(master, EventMetaChangeType.CREATED, emptySet())
                error("공연 저장 중 실패")
            }
        }

        // then
        assertEquals(0, eventMasterJpaRepository.count())
        assertEquals(0, eventOutboxJpaRepository.count())
    }

    @DisplayName("outbox 기록은 공연을 바꾸는 트랜잭션 밖에서는 거부한다")
    @Test
    fun refusesRecordingOutsideTransaction() {
        // given
        val master = eventMasterJpaRepository.save(EventMaster.open(공연("PF-OUT", venue = null)))

        // when
        assertThrows<IllegalTransactionStateException> {
            eventMetaOutboxRecorder.record(master, EventMetaChangeType.UPDATED, setOf("venue"))
        }

        // then
        assertEquals(0, eventOutboxJpaRepository.count())
    }

    @DisplayName("새 공연은 CREATED v1, 장소가 바뀌면 UPDATED v2와 바뀐 필드를 남긴다")
    @Test
    fun recordsCreateAndUpdateWithVersion() {
        // given
        eventMetaTxService.upsert(공연("PF-100", venue = "세종문화회관"))

        // when
        eventMetaTxService.upsert(공연("PF-100", venue = "예술의전당"))

        // then
        val rows = eventOutboxJpaRepository.findAll().sortedBy { it.id }
        assertEquals(listOf("CREATED" to 1L, "UPDATED" to 2L), rows.map { it.eventType to it.metaVersion })
        val updated = objectMapper.readValue(rows.last().payload, EventMetaMessage::class.java)
        assertEquals(listOf("venue"), updated.changedFields)
        assertEquals("예술의전당", updated.snapshot?.venue)
        assertEquals(2L, eventMasterJpaRepository.findByGlobalId(updated.globalId)?.metaVersion)
        assertNull(rows.last().sentAt)
    }

    @DisplayName("내용이 같으면 outbox를 남기지 않는다")
    @Test
    fun skipsWhenNothingChanged() {
        // given
        eventMetaTxService.upsert(공연("PF-200", venue = "세종문화회관"))

        // when
        eventMetaTxService.upsert(공연("PF-200", venue = "세종문화회관"))

        // then
        assertEquals(listOf("CREATED"), eventOutboxJpaRepository.findAll().map { it.eventType })
    }

    private fun 공연(
        sourceId: String,
        venue: String?,
    ): CollectedPerformance =
        CollectedPerformance(
            source = "KOPIS_PBLPRFR",
            sourceId = sourceId,
            title = "공연 $sourceId",
            venue = venue,
            startDate = LocalDate.of(2026, 12, 1),
            endDate = LocalDate.of(2026, 12, 3),
            rawPayload = "$sourceId|$venue",
        )
}
