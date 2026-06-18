package com.sleekydz86.idolglow.schedule.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleSliceResponse

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
                nextCursorId = response.nextCursorId?.asGraphQlId(),
            )
    }
}
