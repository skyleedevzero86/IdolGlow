package com.sleekydz86.idolglow.notification.application.dto

import com.sleekydz86.idolglow.notification.domain.NotificationType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 설정 응답 DTO")
data class NotificationPreferenceResponse(
    @field:Schema(description = "알림 타입", example = "PAYMENT_FAILED")
    val type: NotificationType,
    @field:Schema(description = "수신 여부", example = "true")
    val enabled: Boolean,
)
