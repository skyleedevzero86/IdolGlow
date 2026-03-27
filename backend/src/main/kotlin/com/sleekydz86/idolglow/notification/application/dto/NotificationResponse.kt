package com.sleekydz86.idolglow.notification.application.dto

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림 응답 DTO")
data class NotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "알림 타입", example = "RESERVATION_CONFIRMED")
    val type: NotificationType,
    @field:Schema(description = "알림 제목", example = "예약이 확정되었습니다")
    val title: String,
    @field:Schema(description = "알림 본문", example = "예약이 정상적으로 확정되었습니다.")
    val message: String,
    @field:Schema(description = "이동 링크", example = "/mypage/bookings/1")
    val link: String?,
    @field:Schema(description = "읽은 시각", example = "2026-03-27T19:10:00")
    val readAt: LocalDateTime?,
    @field:Schema(description = "생성 시각", example = "2026-03-27T19:03:00")
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                link = notification.link,
                readAt = notification.readAt,
                createdAt = notification.createdAt
            )
    }
}
