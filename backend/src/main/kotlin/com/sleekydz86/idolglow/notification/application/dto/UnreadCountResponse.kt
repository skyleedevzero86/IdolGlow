package com.sleekydz86.idolglow.notification.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "미읽음 알림 개수 응답 DTO")
data class UnreadCountResponse(
    @field:Schema(description = "미읽음 알림 개수", example = "3")
    val count: Long,
)
