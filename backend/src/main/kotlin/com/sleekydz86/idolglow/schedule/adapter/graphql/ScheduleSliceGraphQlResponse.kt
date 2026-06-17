package com.sleekydz86.idolglow.schedule.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleSliceResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCommandResponse

data class ScheduleSliceGraphQlResponse(
    val schedules: List<ScheduleGraphQlResponse>,
    val hasNext: Boolean,
    val nextCursorId: String?,
) {
    companion object {
        fun from(response: ScheduleSliceResponse): ScheduleSliceGraphQlResponse =
            ScheduleSliceGraphQlResponse(
                schedules = response.schedules.map(ScheduleGraphQlResponse::from),
                hasNext = response.hasNext,
                nextCursorId = response.nextCursorId?.asGraphQlId()
            )
    }
}
