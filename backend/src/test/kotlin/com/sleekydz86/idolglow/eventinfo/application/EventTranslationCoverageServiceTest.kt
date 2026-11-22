package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationCoveragePort
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationLocales
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("공연 번역 누락 현황 계산")
class EventTranslationCoverageServiceTest {
    @DisplayName("쿼리 합이 전체 행사 수와 달라도 예외 없이 쿼리 값을 그대로 돌려준다")
    @Test
    fun inconsistentSumKeepsQueryValues() {
        // given
        val port = mockk<EventTranslationCoveragePort>()
        every { port.countActive() } returns 5L
        every { port.countVisible(any()) } returns 2L
        every { port.countMachineOnly(any()) } returns 1L
        every { port.countMissing(any()) } returns 1L
        every { port.findMissing(any(), any()) } returns emptyList()
        val service = EventTranslationCoverageService(port)

        // when
        val result = service.coverage(sampleLimit = 1_000)

        // then
        assertEquals(EventTranslationLocales.TARGETS, result.map { it.locale })
        assertTrue(result.all { it.missing == 1L && !it.isConsistent() })
        verify(exactly = 1) { port.countActive() }
        verify(exactly = 3) { port.findMissing(any(), EventTranslationCoverageService.MAX_SAMPLE_LIMIT) }
    }
}
