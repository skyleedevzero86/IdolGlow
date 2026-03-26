package com.sleekydz86.idolglow.notification.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.application.NotificationQueryService
import com.sleekydz86.idolglow.notification.application.NotificationStreamService
import com.sleekydz86.idolglow.notification.application.dto.NotificationResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationCommandService: NotificationCommandService,
    private val notificationStreamService: NotificationStreamService,
) {

    @GetMapping
    fun findNotifications(@LoginUser userId: Long): ResponseEntity<List<NotificationResponse>> =
        ResponseEntity.ok(notificationQueryService.findNotifications(userId))

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@LoginUser userId: Long): SseEmitter =
        notificationStreamService.subscribe(userId)

    @PostMapping("/{notificationId}/read")
    fun markRead(
        @LoginUser userId: Long,
        @PathVariable notificationId: Long
    ): ResponseEntity<NotificationResponse> =
        ResponseEntity.ok(
            NotificationResponse.from(
                notificationCommandService.markRead(notificationId, userId)
            )
        )
}
