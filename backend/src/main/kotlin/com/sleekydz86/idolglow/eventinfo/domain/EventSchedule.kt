package com.sleekydz86.idolglow.eventinfo.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class EventSchedule(
    val times: Map<DayOfWeek, Set<LocalTime>>,
    val skipped: List<String>,
) {
    fun expand(
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDateTime> {
        if (from.isAfter(to) || times.isEmpty()) return emptyList()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .flatMap { day -> times[day.dayOfWeek].orEmpty().sorted().map { day.atTime(it) } }
            .toList()
    }

    companion object {
        private val SEGMENT: Regex = Regex("""([^(),]+)\(([^)]*)\)""")
        private val TIME: Regex = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")
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
            val skipped = mutableListOf<String>()
            SEGMENT.findAll(text).forEach { match ->
                val days = days(match.groupValues[1].trim())
                val clock = clockTimes(match.groupValues[2])
                if (days.isEmpty() || clock.isEmpty()) {
                    skipped += match.value.trim()
                } else {
                    days.forEach { times.getOrPut(it) { mutableSetOf() }.addAll(clock) }
                }
            }
            if (times.isEmpty() && skipped.isEmpty() && text.isNotEmpty()) skipped += text
            return EventSchedule(times, skipped)
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
