package com.sleekydz86.idolglow.notification.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.application.NotificationQueryService
import com.sleekydz86.idolglow.notification.application.NotificationStreamService
import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Notification", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationCommandService: NotificationCommandService,
    private val notificationStreamService: NotificationStreamService,
) {

    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 최신순으로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "알림 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = NotificationResponse::class)))]
            )
        ]
    )
    @GetMapping
    fun findNotifications(@Parameter(hidden = true) @LoginUser userId: Long): ResponseEntity<List<NotificationResponse>> =
        ResponseEntity.ok(notificationQueryService.findNotifications(userId))

    @Operation(summary = "실시간 알림 구독", description = "SSE 연결을 열어 새 알림을 실시간으로 수신합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "SSE 구독 연결 성공",
                content = [Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)]
            )
        ]
    )
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@Parameter(hidden = true) @LoginUser userId: Long): SseEmitter =
        notificationStreamService.subscribe(userId)

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "알림 읽음 처리 성공",
                content = [Content(schema = Schema(implementation = NotificationResponse::class))]
            )
        ]
    )
    @PostMapping("/{notificationId}/read")
    fun markRead(
        @Parameter(hidden = true)
        @LoginUser userId: Long,
        @Parameter(description = "읽음 처리할 알림 ID", example = "1")
        @PathVariable notificationId: Long
    ): ResponseEntity<NotificationResponse> =
        ResponseEntity.ok(
            NotificationResponse.from(
                notificationCommandService.markRead(notificationId, userId)
            )
        )
}
