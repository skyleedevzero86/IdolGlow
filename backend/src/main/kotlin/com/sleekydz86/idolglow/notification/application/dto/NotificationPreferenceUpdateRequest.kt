package com.sleekydz86.idolglow.notification.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 설정 변경 요청 DTO")
data class NotificationPreferenceUpdateRequest(
    @field:Schema(description = "수신 여부", example = "false")
    val enabled: Boolean,
)
