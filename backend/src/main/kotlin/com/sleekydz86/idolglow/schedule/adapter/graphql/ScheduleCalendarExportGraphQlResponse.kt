package com.sleekydz86.idolglow.schedule.adapter.graphql

import com.sleekydz86.idolglow.schedule.adapter.web.dto.ScheduleCalendarExportResponse

data class ScheduleCalendarExportGraphQlResponse(
    val googleCalendarUrl: String,
    val icsRelativePath: String,
) {
    companion object {
        fun from(response: ScheduleCalendarExportResponse): ScheduleCalendarExportGraphQlResponse =
            ScheduleCalendarExportGraphQlResponse(
                googleCalendarUrl = response.googleCalendarUrl,
                icsRelativePath = response.icsRelativePath,
            )
    }
}
