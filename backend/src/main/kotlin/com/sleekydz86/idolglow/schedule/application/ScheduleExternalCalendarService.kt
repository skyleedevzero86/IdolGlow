package com.sleekydz86.idolglow.schedule.application

import com.sleekydz86.idolglow.global.config.AppCalendarProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCalendarExportResponse
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ScheduleExternalCalendarService(
    private val calendarProperties: AppCalendarProperties,
    private val publicUrlProperties: AppPublicUrlProperties,
) {

    private val eventZone: ZoneId by lazy { ZoneId.of(calendarProperties.eventZoneId) }

    private val googleUtcFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC)

    fun buildIcsBytes(schedule: ScheduleResponse): ByteArray {
        val ics = ScheduleIcsWriter.build(
            schedule = schedule,
            eventZone = eventZone,
            uidDomain = calendarProperties.uidDomain.trim().ifBlank { "idolglow.app" },
            productPageUrl = productPageUrl(schedule.productId),
        )
        return ScheduleIcsWriter.toUtf8Bytes(ics)
    }

    fun buildGoogleCalendarUrl(schedule: ScheduleResponse): String {
        val startUtc = schedule.startAt.atZone(eventZone).toInstant()
        val endUtc = schedule.endAt.atZone(eventZone).toInstant()
        val dates = "${googleUtcFmt.format(startUtc)}/${googleUtcFmt.format(endUtc)}"
        val details = buildDetailsPlain(schedule)
        val base = "https://calendar.google.com/calendar/render"
        return buildString {
            append(base)
            append("?action=TEMPLATE")
            append("&text=").append(urlEncode(schedule.title))
            append("&dates=").append(urlEncode(dates))
            append("&details=").append(urlEncode(details))
        }
    }

    fun buildExportResponse(schedule: ScheduleResponse): ScheduleCalendarExportResponse =
        ScheduleCalendarExportResponse(
            googleCalendarUrl = buildGoogleCalendarUrl(schedule),
            icsRelativePath = "/schedules/${schedule.scheduleId}/calendar.ics",
        )

    private fun productPageUrl(productId: Long): String? {
        val base = publicUrlProperties.publicBaseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        return "$base/products/$productId"
    }

    private fun buildDetailsPlain(schedule: ScheduleResponse): String {
        val url = productPageUrl(schedule.productId)
        return buildString {
            appendLine("IdolGlow 일정")
            appendLine("상품 ID: ${schedule.productId}")
            if (url != null) {
                appendLine("상품 페이지: $url")
            }
        }.trimEnd()
    }

    private fun urlEncode(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20")
}
