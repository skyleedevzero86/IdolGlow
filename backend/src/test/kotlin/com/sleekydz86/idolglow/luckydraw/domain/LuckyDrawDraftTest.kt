package com.sleekydz86.idolglow.luckydraw.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

@DisplayName("응모 행사 등록 검증")
class LuckyDrawDraftTest {
    private val start = LocalDateTime.of(2026, 12, 1, 11, 0)
    private val end = LocalDateTime.of(2026, 12, 8, 11, 0)

    @DisplayName("D1 당첨자 수 0과 11은 거절한다")
    @ParameterizedTest
    @ValueSource(ints = [0, 11])
    fun rejectsWinnerCountOutOfRange(count: Int) {
        // when
        val error = assertThrows<IllegalArgumentException> { draft(winnerCount = count).validate() }

        // then
        assertEquals("당첨자 수는 1 이상 10 이하여야 합니다.", error.message)
    }

    @DisplayName("D1 당첨자 수 경계 1과 10은 허용한다")
    @ParameterizedTest
    @ValueSource(ints = [1, 10])
    fun allowsWinnerCountBoundary(count: Int) {
        // when
        val result = runCatching { draft(winnerCount = count).validate() }

        // then
        assertEquals(true, result.isSuccess)
    }

    @DisplayName("D2 응모 종료가 시작과 같으면 거절한다")
    @Test
    fun rejectsSameStartAndEnd() {
        // when
        val error = assertThrows<IllegalArgumentException> { draft(endsAt = start).validate() }

        // then
        assertEquals("응모 종료 시각은 시작 시각보다 뒤여야 합니다.", error.message)
    }

    @DisplayName("D3 추첨 시각이 응모 종료보다 1초 앞이면 거절하고 같으면 허용한다")
    @Test
    fun drawAtBoundary() {
        // when
        val error = assertThrows<IllegalArgumentException> { draft(drawAt = end.minusSeconds(1)).validate() }

        // then
        assertEquals("추첨 시각은 응모 종료 시각과 같거나 뒤여야 합니다.", error.message)
        assertDoesNotThrow { draft(drawAt = end).validate() }
    }

    @DisplayName("D4 공백만 있는 제목과 101자 경품 이름은 거절하고 100자는 허용한다")
    @Test
    fun titleLengthBoundary() {
        // when
        val blank = assertThrows<IllegalArgumentException> { draft(title = "   ".trim() + "  ").validate() }
        val tooLong = assertThrows<IllegalArgumentException> { draft(prizeName = "가".repeat(101)).validate() }

        // then
        assertEquals("제목과 경품 이름은 1자 이상 100자 이하여야 합니다.", blank.message)
        assertEquals("제목과 경품 이름은 1자 이상 100자 이하여야 합니다.", tooLong.message)
        assertDoesNotThrow { draft(prizeName = "가".repeat(100)).validate() }
    }

    @DisplayName("D4 길이는 코드 포인트로 센다: 이모지 100개는 허용하고 101개는 거절한다")
    @Test
    fun countsCodePoints() {
        // when
        val error = assertThrows<IllegalArgumentException> { draft(title = "🎁".repeat(101)).validate() }

        // then
        assertEquals("제목과 경품 이름은 1자 이상 100자 이하여야 합니다.", error.message)
        assertDoesNotThrow { draft(title = "🎁".repeat(100)).validate() }
    }

    @DisplayName("D4 경품 설명은 일본어·중국어 1000자는 허용하고 1001자는 거절한다")
    @Test
    fun descriptionLengthBoundary() {
        // when
        val error = assertThrows<IllegalArgumentException> { draft(description = "あ".repeat(1001)).validate() }

        // then
        assertEquals("경품 설명은 1자 이상 1000자 이하여야 합니다.", error.message)
        assertDoesNotThrow { draft(description = "票".repeat(1000)).validate() }
    }

    private fun draft(
        title: String = "가요대전 응모",
        prizeName: String = "티켓 2매",
        description: String = "2층 지정석",
        winnerCount: Int = 1,
        endsAt: LocalDateTime = end,
        drawAt: LocalDateTime = end.plusHours(2),
    ): LuckyDrawDraft = LuckyDrawDraft(title, prizeName, description, winnerCount, start, endsAt, drawAt)
}
