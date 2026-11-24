package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceSourceBatchPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedBatch
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("공연 공급처 수집 서비스")
class EventMetaCollectServiceTest {
    private val txService = mockk<EventMetaTxService>()
    private val sourceRefPort = mockk<EventSourceRefPort>()
    private val from = LocalDate.of(2026, 11, 19)
    private val to = LocalDate.of(2026, 12, 19)

    @DisplayName("공급처가 모두 받으면 SUCCESS와 건수를 기록한다")
    @Test
    fun recordsSuccessWhenBatchIsComplete() {

        // given
        준비()
        val service = 서비스(고정_공급처(CollectedBatch(listOf(공연("1"), 공연("2")))))

        // when
        service.collect(from, to)

        // then
        verify(exactly = 1) { txService.recordRun(SOURCE, EventCollectStatus.SUCCESS, 2, null) }
    }

    @DisplayName("일부 호출이 빠지면 받은 건수는 저장하고 PARTIAL과 사유를 기록한다")
    @Test
    fun recordsPartialWhenBatchIsIncomplete() {

        // given
        준비()
        val service = 서비스(고정_공급처(CollectedBatch(listOf(공연("1")), "30일 중 3일 누락")))

        // when
        service.collect(from, to)

        // then
        verify(exactly = 1) { txService.upsert(any()) }
        verify(exactly = 1) { txService.recordRun(SOURCE, EventCollectStatus.PARTIAL, 1, "30일 중 3일 누락") }
    }

    @DisplayName("공급처 호출이 예외로 끝나면 FAILED와 원인을 기록한다")
    @Test
    fun recordsFailedWhenBatchThrows() {

        // given
        준비()
        val failing =
            object : PerformanceSourceBatchPort {
                override val source: String = SOURCE

                override fun fetch(
                    from: LocalDate,
                    to: LocalDate,
                ): List<CollectedPerformance> = error("세종문화회관 API 오류 응답. code=INFO-100")
            }
        val service = 서비스(failing)

        // when
        service.collect(from, to)

        // then
        verify(exactly = 1) {
            txService.recordRun(SOURCE, EventCollectStatus.FAILED, 0, "세종문화회관 API 오류 응답. code=INFO-100")
        }
    }

    private fun 준비() {
        every { txService.upsert(any()) } just runs
        every { txService.recordRun(any(), any(), any(), any()) } just runs
        every { sourceRefPort.countByMatchStatus(any()) } returns 0
    }

    private fun 서비스(source: PerformanceSourceBatchPort): EventMetaCollectService =
        EventMetaCollectService(listOf(source), txService, sourceRefPort)

    private fun 고정_공급처(batch: CollectedBatch): PerformanceSourceBatchPort =
        object : PerformanceSourceBatchPort {
            override val source: String = SOURCE

            override fun fetch(
                from: LocalDate,
                to: LocalDate,
            ): List<CollectedPerformance> = batch.items

            override fun fetchBatch(
                from: LocalDate,
                to: LocalDate,
            ): CollectedBatch = batch
        }

    private fun 공연(sourceId: String): CollectedPerformance =
        CollectedPerformance(
            source = SOURCE,
            sourceId = sourceId,
            title = "공연 $sourceId",
            venue = null,
            startDate = from,
            endDate = to,
            rawPayload = sourceId,
        )

    companion object {
        private const val SOURCE: String = "TEST_SOURCE"
    }
}
