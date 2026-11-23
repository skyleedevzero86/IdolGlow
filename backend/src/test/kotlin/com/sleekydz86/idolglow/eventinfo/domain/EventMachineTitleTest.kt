package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("AI 공연 제목 만들기와 검사")
class EventMachineTitleTest {
    @DisplayName("일문·중문은 전각 괄호, 영문은 반각 괄호로 한국어 원래 제목을 붙인다")
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "ja|ソウル灯籠祭り|ソウル灯籠祭り（서울빛초롱축제）",
            "zh|首尔灯笼节|首尔灯笼节（서울빛초롱축제）",
            "en|Seoul Lantern Festival|Seoul Lantern Festival (서울빛초롱축제)",
        ],
    )
    fun composesWithOriginalTitle(
        locale: String,
        response: String,
        expected: String,
    ) {
        // when
        val title = EventMachineTitle.compose(locale, response, "서울빛초롱축제")

        // then
        assertEquals(expected, title)
    }

    @DisplayName("대상 언어로 번역되고 숫자가 그대로면 통과한다")
    @Test
    fun passesValidTranslation() {
        // when
        val problems = EventMachineTitle.check("ja", "2026 서울빛초롱축제", "2026 ソウル灯籠祭り", emptyList())

        // then
        assertEquals(emptyList<String>(), problems)
    }

    @DisplayName("검사 하나만 어긴 응답은 그 이유로 실패한다")
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "EMPTY|ja|서울빛초롱축제|''",
            "SCRIPT_MISSING|ja|서울빛초롱축제|Seoul Lantern Festival",
            "WRONG_SCRIPT|zh|서울빛초롱축제|ソウル灯笼节",
            "HANGUL_LEFT|en|서울빛초롱축제|Seoul 빛초롱 Festival",
            "NUMBER_CHANGED|en|2026 서울빛초롱축제|2025 Seoul Lantern Festival",
            "REFUSAL|en|서울빛초롱축제|I'm sorry, I cannot translate this",
        ],
    )
    fun failsWithReason(
        reason: String,
        locale: String,
        korean: String,
        response: String,
    ) {
        // when
        val problems = EventMachineTitle.check(locale, korean, response, emptyList())

        // then
        assertTrue(problems.any { it.startsWith(reason) }, "problems=$problems")
    }

    @DisplayName("원래 제목을 붙인 저장 제목이 255자면 통과하고 256자면 TOO_LONG으로 실패한다")
    @Test
    fun lengthBoundaryCountsCharacters() {
        // given
        val korean = "축제"
        val suffix = "（축제）".length
        val fits = "祭".repeat(EventTranslation.TITLE_MAX - suffix)
        val tooLong = "祭".repeat(EventTranslation.TITLE_MAX - suffix) + "😀"

        // when
        val fitting = EventMachineTitle.check("ja", korean, fits, emptyList())
        val overflowing = EventMachineTitle.check("ja", korean, tooLong, emptyList())

        // then
        assertEquals(emptyList<String>(), fitting.filter { it.startsWith("TOO_LONG") })
        assertTrue(overflowing.any { it.startsWith("TOO_LONG") }, "problems=$overflowing")
    }
}
