package com.sleekydz86.idolglow.global.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("날짜 표시 포맷")
class IdolGlowDateTimesTest {
    @DisplayName("조회 구간은 년-월-일 시:분으로 보여 준다")
    @Test
    fun displaysLocalDateTimeWithSpaceAndColon() {
        // given
        val value = LocalDateTime.of(2026, 11, 1, 0, 0, 0)

        // when
        val displayed = IdolGlowDateTimes.toDisplay(value)

        // then
        assertEquals("2026-11-01 00:00", displayed)
    }

    @DisplayName("토스 ISO 시각도 서울 로컬 시각으로 읽는다")
    @Test
    fun parsesTossIsoOffsetIntoSeoulLocalDateTime() {
        // given
        val raw = "2026-11-01T00:00:00+09:00"

        // when
        val parsed = IdolGlowDateTimes.parseDateTime(raw)

        // then
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0, 0), parsed)
        assertEquals("2026-11-01 00:00", IdolGlowDateTimes.toDisplay(parsed))
    }
}
