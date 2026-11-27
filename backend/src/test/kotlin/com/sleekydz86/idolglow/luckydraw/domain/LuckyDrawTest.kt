package com.sleekydz86.idolglow.luckydraw.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.luckydraw.LuckyDrawExceptionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("응모 행사 상태와 전환")
class LuckyDrawTest {
    private val start = LocalDateTime.of(2026, 12, 1, 11, 0)
    private val end = LocalDateTime.of(2026, 12, 8, 11, 0)
    private val drawAt = LocalDateTime.of(2026, 12, 8, 13, 0)

    @DisplayName("상태는 시작 1초 전 SCHEDULED, 시작 정각 OPEN, 종료 1초 전 OPEN, 종료 정각 CLOSED")
    @Test
    fun statusFollowsHalfOpenPeriod() {
        // given
        val draw = draw()

        // when
        val statuses = listOf(start.minusSeconds(1), start, end.minusSeconds(1), end).map { draw.status(it) }

        // then
        assertEquals(
            listOf(LuckyDrawStatus.SCHEDULED, LuckyDrawStatus.OPEN, LuckyDrawStatus.OPEN, LuckyDrawStatus.CLOSED),
            statuses,
        )
    }

    @DisplayName("추첨을 마치면 DRAWN 이고 씨앗이 남으며, 두 번째 completeDraw 는 거절한다")
    @Test
    fun completeDrawOnce() {
        // given
        val draw = draw()

        // when
        draw.completeDraw(seed = 42L, now = drawAt)
        val second = assertThrows<IllegalStateException> { draw.completeDraw(seed = 7L, now = drawAt.plusMinutes(1)) }

        // then
        assertEquals(LuckyDrawStatus.DRAWN, draw.status(drawAt))
        assertEquals(42L, draw.drawSeed)
        assertEquals(true, second.message?.startsWith("이미 추첨을 마친 추첨입니다."))
    }

    @DisplayName("추첨 시각 전에는 completeDraw 를 거절한다")
    @Test
    fun rejectsEarlyDraw() {
        // given
        val draw = draw()

        // when
        val error = assertThrows<IllegalStateException> { draw.completeDraw(seed = 1L, now = drawAt.minusSeconds(1)) }

        // then
        assertEquals(true, error.message?.startsWith("추첨 시각이 지나지 않았습니다."))
    }

    @DisplayName("D6 응모 시작 정각부터는 수정할 수 없다")
    @Test
    fun editableOnlyBeforeStart() {
        // given
        val draw = draw()

        // when
        val error = assertThrows<CustomException> { draw.assertEditable(start) }

        // then
        assertEquals(LuckyDrawExceptionType.ALREADY_STARTED, error.getExceptionType())
        draw.assertEditable(start.minusSeconds(1))
    }

    @DisplayName("D8 OPEN 이 아니면 응모를 거절한다")
    @Test
    fun assertOpenRejectsClosed() {
        // given
        val draw = draw()

        // when
        val error = assertThrows<CustomException> { draw.assertOpen(end) }

        // then
        assertEquals(LuckyDrawExceptionType.NOT_OPEN, error.getExceptionType())
    }

    @DisplayName("D5 기간이 맞닿으면 겹치지 않고, 1초라도 겹치면 겹친다")
    @Test
    fun overlapBoundary() {
        // given
        val draw = draw()
        val touching = LuckyDraw.create(LuckyDrawDraft("다음", "경품", "설명", 1, end, end.plusDays(7), end.plusDays(7)))
        val overlapping =
            LuckyDraw.create(
                LuckyDrawDraft("겹침", "경품", "설명", 1, end.minusSeconds(1), end.plusDays(7), end.plusDays(7)),
            )

        // when
        val touches = draw.overlaps(touching)
        val overlaps = draw.overlaps(overlapping)

        // then
        assertFalse(touches)
        assertTrue(overlaps)
    }

    private fun draw(): LuckyDraw = LuckyDraw.create(LuckyDrawDraft("응모", "경품", "설명", 2, start, end, drawAt))
}
