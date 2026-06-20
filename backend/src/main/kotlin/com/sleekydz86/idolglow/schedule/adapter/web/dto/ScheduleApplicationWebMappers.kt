package com.sleekydz86.idolglow.schedule.adapter.web.dto

import com.sleekydz86.idolglow.schedule.application.dto.ScheduleCalendarExportResult

fun ScheduleCalendarExportResult.toWebResponse(): ScheduleCalendarExportResponse =
    ScheduleCalendarExportResponse(
        googleCalendarUrl = googleCalendarUrl,
        icsRelativePath = icsRelativePath,
    )
