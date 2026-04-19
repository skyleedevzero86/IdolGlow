package com.sleekydz86.idolglow.admin.ui.request

import com.sleekydz86.idolglow.subscription.application.dto.UpsertSubscriptionDispatchScheduleCommand
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.DayOfWeek

data class UpsertSubscriptionDispatchScheduleRequest(
    val frequencyType: SubscriptionDispatchFrequency,
    val dayOfWeek: DayOfWeek? = null,
    @field:NotBlank
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "발송 시간은 HH:mm 형식이어야 합니다.")
    val dispatchTime: String,
    val active: Boolean = true,
) {
    fun toCommand(contentType: SubscriptionContentType): UpsertSubscriptionDispatchScheduleCommand =
        UpsertSubscriptionDispatchScheduleCommand(
            contentType = contentType,
            frequencyType = frequencyType,
            dayOfWeek = dayOfWeek,
            dispatchTime = dispatchTime,
            active = active,
        )
}
