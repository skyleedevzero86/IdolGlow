package com.sleekydz86.idolglow.schedule.application

import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ScheduleIcsWriter {

    private val utcFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC)

    fun build(
        schedule: ScheduleResponse,
        eventZone: ZoneId,
        uidDomain: String,
        productPageUrl: String?,
    ): String {
        val startUtc = schedule.startAt.atZone(eventZone).toInstant()
        val endUtc = schedule.endAt.atZone(eventZone).toInstant()
        val dtStamp = ZonedDateTime.now(java.time.ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
        val uid = "idolglow-schedule-${schedule.scheduleId}@$uidDomain"
        val summary = escapeIcsText(schedule.title)
        val description = buildDescription(schedule, productPageUrl)
        val urlLine = productPageUrl?.let { "URL:${escapeIcsText(it)}" }

        val blocks = mutableListOf<String>()
        blocks += "BEGIN:VCALENDAR"
        blocks += "VERSION:2.0"
        blocks += "PRODID:-//IdolGlow//Schedule//KO"
        blocks += "CALSCALE:GREGORIAN"
        blocks += "METHOD:PUBLISH"
        blocks += "BEGIN:VEVENT"
        blocks += foldProperty("UID", uid)
        blocks += "DTSTAMP:${utcFmt.format(dtStamp)}"
        blocks += "DTSTART:${utcFmt.format(startUtc)}"
        blocks += "DTEND:${utcFmt.format(endUtc)}"
        blocks += foldProperty("SUMMARY", summary)
        blocks += foldProperty("DESCRIPTION", description)
        if (urlLine != null) {
            blocks += foldRawProperty(urlLine)
        }
        blocks += "SEQUENCE:0"
        blocks += "STATUS:CONFIRMED"
        blocks += valarm("-P1D", "IdolGlow: 내일 방문 예정입니다.")
        blocks += valarm("-PT3H", "IdolGlow: 3시간 뒤 방문입니다.")
        blocks += valarm("-PT1H", "IdolGlow: 1시간 뒤 방문입니다.")
        blocks += "END:VEVENT"
        blocks += "END:VCALENDAR"
        return blocks.joinToString("\r\n") + "\r\n"
    }

    fun toUtf8Bytes(ics: String): ByteArray = ics.toByteArray(StandardCharsets.UTF_8)

    private fun buildDescription(schedule: ScheduleResponse, productPageUrl: String?): String {
        val lines = mutableListOf<String>()
        lines += "IdolGlow 일정"
        lines += "상품 ID: ${schedule.productId}"
        if (!productPageUrl.isNullOrBlank()) {
            lines += "상품 페이지: $productPageUrl"
        }
        return escapeIcsText(lines.joinToString("\n"))
    }

    private fun valarm(trigger: String, description: String): String =
        listOf(
            "BEGIN:VALARM",
            "TRIGGER:$trigger",
            "ACTION:DISPLAY",
            foldProperty("DESCRIPTION", escapeIcsText(description)),
            "END:VALARM",
        ).joinToString("\r\n")

    private fun foldProperty(name: String, escapedValue: String): String =
        foldRawProperty("$name:$escapedValue")

    private fun foldRawProperty(line: String): String {
        val bytes = line.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size <= 75) {
            return line
        }
        val out = StringBuilder()
        var i = 0
        var first = true
        while (i < bytes.size) {
            val maxChunk = if (first) 75 else 74
            var end = minOf(i + maxChunk, bytes.size)
            if (!first) {
                out.append("\r\n ")
            }
            while (end > i && !isUtf8CharBoundary(bytes, end)) {
                end--
            }
            if (end == i) {
                end = minOf(i + 1, bytes.size)
            }
            out.append(String(bytes, i, end - i, StandardCharsets.UTF_8))
            i = end
            first = false
        }
        return out.toString()
    }

    private fun isUtf8CharBoundary(arr: ByteArray, index: Int): Boolean {
        if (index <= 0 || index >= arr.size) return true
        val b = arr[index].toInt() and 0xff
        return b and 0xC0 != 0x80
    }

    private fun escapeIcsText(s: String): String =
        s.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
}
