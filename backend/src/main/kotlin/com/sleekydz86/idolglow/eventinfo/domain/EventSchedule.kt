package com.sleekydz86.idolglow.eventinfo.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class EventSchedule(
    val times: Map<DayOfWeek, Set<LocalTime>>,
    val skipped: List<String>,
    val holidayTimes: Set<LocalTime> = emptySet(),
) {
    fun expand(
        from: LocalDate,
        to: LocalDate,
        isHoliday: (LocalDate) -> Boolean = { false },
    ): List<LocalDateTime> {
        if (from.isAfter(to) || (times.isEmpty() && holidayTimes.isEmpty())) return emptyList()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .flatMap { day -> timesOn(day, isHoliday).sorted().map { day.atTime(it) } }
            .toList()
    }

    private fun timesOn(
        day: LocalDate,
        isHoliday: (LocalDate) -> Boolean,
    ): Set<LocalTime> =
        if (holidayTimes.isNotEmpty() && isHoliday(day)) holidayTimes else times[day.dayOfWeek].orEmpty()

    companion object {
        private val SEGMENT: Regex = Regex("""([^(),]+)\(([^)]*)\)""")
        private val TIME: Regex = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")
        private const val HOLIDAY: String = "HOL"
        private val DAYS: Map<Char, DayOfWeek> =
            mapOf(
                '월' to DayOfWeek.MONDAY,
                '화' to DayOfWeek.TUESDAY,
                '수' to DayOfWeek.WEDNESDAY,
                '목' to DayOfWeek.THURSDAY,
                '금' to DayOfWeek.FRIDAY,
                '토' to DayOfWeek.SATURDAY,
                '일' to DayOfWeek.SUNDAY,
            )

        fun parse(guidance: String?): EventSchedule {
            val text = guidance?.trim().orEmpty()
            val times = mutableMapOf<DayOfWeek, MutableSet<LocalTime>>()
            val holidayTimes = mutableSetOf<LocalTime>()
            val skipped = mutableListOf<String>()
            SEGMENT.findAll(text).forEach { match ->
                val name = match.groupValues[1].trim()
                val days = days(name)
                val clock = clockTimes(match.groupValues[2])
                when {
                    clock.isEmpty() -> skipped += match.value.trim()
                    name.equals(HOLIDAY, ignoreCase = true) -> holidayTimes.addAll(clock)
                    days.isEmpty() -> skipped += match.value.trim()
                    else -> days.forEach { times.getOrPut(it) { mutableSetOf() }.addAll(clock) }
                }
            }
            val nothingRead = times.isEmpty() && holidayTimes.isEmpty()
            if (nothingRead && skipped.isEmpty() && text.isNotEmpty()) skipped += text
            return EventSchedule(times, skipped, holidayTimes)
        }

        private fun days(spec: String): List<DayOfWeek> {
            val parts = spec.split('~').map { day(it.trim()) }
            if (parts.size > 2 || parts.any { it == null }) return emptyList()
            val first = checkNotNull(parts.first())
            val last = checkNotNull(parts.last())
            val length = (last.value - first.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
            return (0..length).map { first.plus(it.toLong()) }
        }

        private fun day(word: String): DayOfWeek? {
            val name = word.removeSuffix("요일")
            return if (name.length == 1) DAYS[name.single()] else null
        }

        private fun clockTimes(raw: String): Set<LocalTime> {
            val values = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val parsed = values.mapNotNull { value -> TIME.find(value)?.let { LocalTime.of(it.int(1), it.int(2)) } }
            return if (parsed.size == values.size) parsed.toSet() else emptySet()
        }

        private fun MatchResult.int(group: Int): Int = groupValues[group].toInt()

        private const val DAYS_IN_WEEK: Int = 7
    }
}
