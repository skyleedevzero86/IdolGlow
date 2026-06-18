package com.sleekydz86.idolglow.schedule.application

import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleIcsWriterTest {
    @Test
    fun `ics_본문에_uid_요약_알람_정보가_포함된다`() {
        // given
        val schedule =
            ScheduleResponse(
                scheduleId = 42L,
                productId = 7L,
                title = "스튜디오 방문",
                startAt = LocalDateTime.of(2026, 5, 1, 9, 0),
                endAt = LocalDateTime.of(2026, 5, 1, 10, 0),
            )

        // when
        val ics =
            ScheduleIcsWriter.build(
                schedule = schedule,
                eventZone = ZoneId.of("Asia/Seoul"),
                uidDomain = "test.local",
                productPageUrl = "https://app.example/products/7",
            )

        // then
        assertTrue(ics.contains("UID:idolglow-schedule-42@test.local"))
        assertTrue(ics.contains("BEGIN:VALARM"))
        assertTrue(ics.contains("TRIGGER:-P1D"))
        assertTrue(ics.contains("TRIGGER:-PT3H"))
        assertTrue(ics.contains("TRIGGER:-PT1H"))
        assertTrue(ics.contains("ACTION:DISPLAY"))
        assertTrue(ics.endsWith("\r\n"))
    }
}
