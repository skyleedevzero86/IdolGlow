package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("KOPIS 공연·축제 수집 어댑터")
class KopisPerformanceBatchAdapterTest {
    private val client = mockk<KopisPerformanceApiClient>()
    private val from = LocalDate.of(2026, 11, 20)
    private val to = LocalDate.of(2026, 12, 20)

    @DisplayName("모든 쪽을 받으면 사유 없이 돌려준다")
    @Test
    fun returnsCompleteBatchWhenAllPagesLoad() {
        // given
        every { client.listPerformancesForBatch(any(), any(), 1, 100) } returns 공연들(1, 30)

        // when
        val batch = KopisPerformanceBatchAdapter(client).fetchBatch(from, to)

        // then
        assertEquals(30, batch.items.size)
        assertNull(batch.incompleteReason)
    }

    @DisplayName("두 번째 쪽이 실패하면 받은 공연은 두고 일부 수집 사유를 남긴다")
    @Test
    fun returnsPartialBatchWhenLaterPageFails() {
        // given
        every { client.listFestivalsForBatch(any(), any(), 1, 100) } returns 공연들(1, 100)
        every { client.listFestivalsForBatch(any(), any(), 2, 100) } throws
            IllegalStateException("KOPIS 호출 실패. path=prffest status=400")

        // when
        val batch = KopisFestivalBatchAdapter(client).fetchBatch(from, to)

        // then
        assertEquals(100, batch.items.size)
        assertEquals(
            "2쪽 호출이 실패해 그 뒤 공연이 빠졌습니다. KOPIS 호출 실패. path=prffest status=400",
            batch.incompleteReason,
        )
    }

    @DisplayName("첫 쪽부터 실패하면 예외를 그대로 알려 실패로 기록되게 한다")
    @Test
    fun throwsWhenFirstPageFails() {
        // given
        every { client.listPerformancesForBatch(any(), any(), 1, 100) } throws
            IllegalStateException("KOPIS 호출 실패. path=pblprfr status=400")

        // when
        val error = assertThrows<IllegalStateException> { KopisPerformanceBatchAdapter(client).fetchBatch(from, to) }

        // then
        assertEquals("KOPIS 호출 실패. path=pblprfr status=400", error.message)
    }

    private fun 공연들(
        first: Int,
        count: Int,
    ): List<FestivalEvent> =
        (first until first + count).map { index ->
            FestivalEvent(
                contentId = "PF$index",
                title = "공연 $index",
                address = null,
                eventStartDate = "20261120",
                eventEndDate = "20261220",
                thumbnailImageUrl = null,
                imageUrl = null,
                mapX = null,
                mapY = null,
                phone = null,
            )
        }
}
