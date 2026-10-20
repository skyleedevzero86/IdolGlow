package com.sleekydz86.idolglow.global.support

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Suppress("TooManyFunctions")
object IdolGlowDateTimes {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    val STORAGE_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(STORAGE_DATE_TIME_PATTERN)
    val STORAGE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern(STORAGE_DATE_PATTERN)
    val STORAGE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(STORAGE_TIME_PATTERN)
    val DISPLAY_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DISPLAY_DATE_TIME_PATTERN)
    val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern(DISPLAY_DATE_PATTERN)
    val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DISPLAY_TIME_PATTERN)

    fun toDisplay(value: LocalDateTime?): String? = value?.format(DISPLAY_DATE_TIME)

    fun toDisplay(value: LocalDate?): String? = value?.format(DISPLAY_DATE)

    fun toDisplay(value: LocalTime?): String? = value?.format(DISPLAY_TIME)

    fun toDisplay(value: Instant?): String? = toDisplay(value?.atZone(ZONE)?.toLocalDateTime())

    fun toDisplay(value: OffsetDateTime?): String? = toDisplay(value?.atZoneSameInstant(ZONE)?.toLocalDateTime())

    fun parseDateTime(raw: String?): LocalDateTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return parseExactDateTime(value) ?: parseOffsetDateTime(value) ?: parseDate(value)?.atStartOfDay()
    }

    fun parseDate(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { LocalDate.parse(value, DISPLAY_DATE) }.getOrNull()
            ?: runCatching { LocalDate.parse(value, STORAGE_DATE) }.getOrNull()
            ?: runCatching { LocalDate.parse(value) }.getOrNull()
            ?: parseDateTime(value)?.toLocalDate()
    }

    fun parseTime(raw: String?): LocalTime? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { LocalTime.parse(value, DISPLAY_TIME) }.getOrNull()
            ?: runCatching { LocalTime.parse(value, STORAGE_TIME) }.getOrNull()
            ?: runCatching { LocalTime.parse(value) }.getOrNull()
            ?: parseDateTime(value)?.toLocalTime()
    }

    fun parseInstant(raw: String?): Instant? = parseDateTime(raw)?.atZone(ZONE)?.toInstant()

    private fun parseExactDateTime(value: String): LocalDateTime? =
        DATE_TIME_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
        }

    private fun parseOffsetDateTime(value: String): LocalDateTime? =
        try {
            OffsetDateTime.parse(value).atZoneSameInstant(ZONE).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            runCatching { Instant.parse(value).atZone(ZONE).toLocalDateTime() }.getOrNull()
        }

    private val DATE_TIME_FORMATTERS =
        listOf(
            DISPLAY_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"),
            STORAGE_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        )

    const val STORAGE_DATE_TIME_PATTERN = "yyyyMMddHHmmss"
    const val STORAGE_DATE_PATTERN = "yyyyMMdd"
    const val STORAGE_TIME_PATTERN = "HHmmss"
    const val DISPLAY_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
    const val DISPLAY_DATE_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_TIME_PATTERN = "HH:mm"
}
