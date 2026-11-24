package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaProjection
import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("병합 대상 칸 merged_into 읽기 (MySQL)")
class EventMergedIntoMysqlTest : MysqlIntegrationTest() {
    @Autowired
    private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired
    private lateinit var eventMetaProjectionJpaRepository: EventMetaProjectionJpaRepository

    @Autowired
    private lateinit var eventOutboxJpaRepository: EventOutboxJpaRepository

    @Autowired
    private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired
    private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired
    private lateinit var eventTranslationAttemptJpaRepository: EventTranslationAttemptJpaRepository

    @BeforeEach
    fun clean() {
        eventOutboxJpaRepository.deleteAllInBatch()
        eventTranslationAttemptJpaRepository.deleteAllInBatch()
        eventTranslationJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventMetaProjectionJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("병합된 대표 공연을 다시 읽으면 상태와 이어받은 공연 ID가 그대로 나온다")
    @Test
    fun readsMergedMaster() {
        // given
        val target = master("g-target")
        val merged = master("g-merged")
        merged.mergeInto(target)
        eventMasterJpaRepository.saveAndFlush(merged)

        // when
        val stored = eventMasterJpaRepository.findByGlobalId("g-merged")!!

        // then
        assertEquals(EventMasterStatus.MERGED, stored.status)
        assertEquals("g-target", stored.mergedInto)
    }

    @DisplayName("공연 메타 읽기 모델을 다시 읽으면 이어받은 공연 ID가 그대로 나온다")
    @Test
    fun readsMergedProjection() {
        // given
        val target = master("g-target")
        val merged = master("g-merged")
        merged.mergeInto(target)
        eventMetaProjectionJpaRepository.saveAndFlush(EventMetaProjection.from("g-merged", 2L, merged.snapshot()))

        // when
        val stored = eventMetaProjectionJpaRepository.findById("g-merged").get()

        // then
        assertEquals("MERGED", stored.status)
        assertEquals("g-target", stored.mergedInto)
    }

    private fun master(globalId: String): EventMaster =
        eventMasterJpaRepository.saveAndFlush(
            EventMaster(
                globalId = globalId,
                title = "병합 확인 공연 $globalId",
                normalizedTitle = "병합 확인 공연 $globalId",
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 2),
                venue = null,
                normalizedVenue = null,
                status = EventMasterStatus.ACTIVE,
            ),
        )
}
