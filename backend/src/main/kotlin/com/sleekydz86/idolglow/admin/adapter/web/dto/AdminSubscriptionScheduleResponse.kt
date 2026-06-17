package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDateTime

data class AdminSubscriptionScheduleResponse(
    val id: Long,
    val contentType: String,
    val contentTypeLabel: String,
    val frequencyType: String,
    val frequencyTypeLabel: String,
    val dayOfWeek: String?,
    val dayOfWeekLabel: String?,
    val dispatchTime: String,
    val active: Boolean,
    val nextDispatchAt: String?,
) {
    companion object {
        fun from(schedule: SubscriptionDispatchSchedule): AdminSubscriptionScheduleResponse =
            AdminSubscriptionScheduleResponse(
                id = schedule.id,
                contentType = schedule.contentType.name,
                contentTypeLabel = schedule.contentType.label,
                frequencyType = schedule.frequencyType.name,
                frequencyTypeLabel = schedule.frequencyType.label,
                dayOfWeek = schedule.dayOfWeek?.name,
                dayOfWeekLabel = schedule.dayOfWeek?.toKoreanLabel(),
                dispatchTime = "%02d:%02d".format(schedule.dispatchHour, schedule.dispatchMinute),
                active = schedule.active,
                nextDispatchAt = schedule.nextDispatchDateTime()?.asSubscriptionDateTime(),
            )
    }
}
