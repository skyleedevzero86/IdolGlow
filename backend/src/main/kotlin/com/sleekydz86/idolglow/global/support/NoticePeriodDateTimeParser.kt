package com.sleekydz86.idolglow.global.support

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NoticePeriodDateTimeParser {
    fun parse(raw: String?): LocalDateTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return null
        }

        parseDate(value, isStart = true)?.let { return it }
        parseDate(value, isStart = false)?.let { return it }

        return DATE_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }
    }

    fun isWithinPeriod(
        noticeStartDate: String?,
        noticeEndDate: String?,
        now: LocalDateTime,
    ): Boolean {
        val start = parse(noticeStartDate)
        val end = parse(noticeEndDate)

        if (start != null && now.isBefore(start)) {
            return false
        }

        if (end != null && now.isAfter(end)) {
            return false
        }

        return true
    }

    private fun parseDate(value: String, isStart: Boolean): LocalDateTime? {
        return DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { java.time.LocalDate.parse(value, formatter) }
                .map { if (isStart) it.atStartOfDay() else it.atTime(23, 59, 59) }
                .getOrNull()
        }
    }

    private val DATE_TIME_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyyMMddHHmm"),
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    )

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    )
}
