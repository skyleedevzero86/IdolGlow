package com.sleekydz86.idolglow.schedule.graphql

import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCalendarExportResponse

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
