package com.sleekydz86.idolglow.schedule.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCommandResponse

data class ScheduleGraphQlResponse(
    val scheduleId: String,
    val productId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
) {
    companion object {
        fun from(response: ScheduleResponse): ScheduleGraphQlResponse =
            ScheduleGraphQlResponse(
                scheduleId = response.scheduleId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                title = response.title,
                startAt = requireNotNull(response.startAt.asGraphQlValue()),
                endAt = requireNotNull(response.endAt.asGraphQlValue()),
            )

        fun from(response: ScheduleCommandResponse): ScheduleGraphQlResponse =
            ScheduleGraphQlResponse(
                scheduleId = response.scheduleId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                title = response.title,
                startAt = requireNotNull(response.startAt.asGraphQlValue()),
                endAt = requireNotNull(response.endAt.asGraphQlValue()),
            )
    }
}
