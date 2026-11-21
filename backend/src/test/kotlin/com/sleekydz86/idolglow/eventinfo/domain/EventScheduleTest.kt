package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("KOPIS 공연 시간 안내 문장 읽기와 회차 펼치기")
class EventScheduleTest {
    @DisplayName("요일 범위와 여러 시각을 읽는다")
    @Test
    fun parsesRangesAndTimes() {
        // when
        val schedule = EventSchedule.parse("화요일 ~ 금요일(20:00), 토요일 ~ 일요일(15:00,19:00)")

        // then
        assertEquals(setOf(LocalTime.of(20, 0)), schedule.times[DayOfWeek.TUESDAY])
        assertEquals(setOf(LocalTime.of(20, 0)), schedule.times[DayOfWeek.FRIDAY])
        assertEquals(setOf(LocalTime.of(15, 0), LocalTime.of(19, 0)), schedule.times[DayOfWeek.SUNDAY])
        assertEquals(null, schedule.times[DayOfWeek.MONDAY])
        assertEquals(emptyList<String>(), schedule.skipped)
    }

    @DisplayName("금요일 ~ 월요일처럼 주를 넘어가는 범위도 읽는다")
    @Test
    fun parsesWrappingRange() {
        // when
        val schedule = EventSchedule.parse("금요일 ~ 월요일(19:30)")

        // then
        assertEquals(
            setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
            schedule.times.keys,
        )
    }

    @DisplayName("공휴일 표시와 읽을 수 없는 시각은 건너뛰고 따로 남긴다")
    @Test
    fun skipsHolidayAndBadTimes() {
        // when
        val schedule = EventSchedule.parse("수요일(14:00), HOL(15:00), 목요일(오후 3시)")

        // then
        assertEquals(setOf(DayOfWeek.WEDNESDAY), schedule.times.keys)
        assertEquals(listOf("HOL(15:00)", "목요일(오후 3시)"), schedule.skipped)
    }

    @DisplayName("형식이 전혀 다른 문장은 회차 없이 통째로 건너뛴다")
    @Test
    fun skipsUnknownSentence() {
        // when
        val schedule = EventSchedule.parse("공연 시간은 예매처에 문의")
        val empty = EventSchedule.parse(null)

        // then
        assertEquals(emptyMap<DayOfWeek, Set<LocalTime>>(), schedule.times)
        assertEquals(listOf("공연 시간은 예매처에 문의"), schedule.skipped)
        assertEquals(emptyList<String>(), empty.skipped)
    }

    @DisplayName("기간 안의 요일마다 시각 순서대로 회차를 펼친다")
    @Test
    fun expandsWithinPeriod() {
        // given
        val schedule = EventSchedule.parse("토요일(19:00,15:00)")

        // when
        val sessions = schedule.expand(LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 13))
        val reversed = schedule.expand(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 4))

        // then
        assertEquals(
            listOf(
                LocalDate.of(2026, 12, 5).atTime(15, 0),
                LocalDate.of(2026, 12, 5).atTime(19, 0),
                LocalDate.of(2026, 12, 12).atTime(15, 0),
                LocalDate.of(2026, 12, 12).atTime(19, 0),
            ),
            sessions,
        )
        assertEquals(0, reversed.size)
    }
}
