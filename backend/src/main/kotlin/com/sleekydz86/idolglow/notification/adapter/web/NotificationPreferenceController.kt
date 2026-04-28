package com.sleekydz86.idolglow.notification.ui

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.notification.application.NotificationPreferenceService
import com.sleekydz86.idolglow.notification.application.dto.NotificationPreferenceResponse
import com.sleekydz86.idolglow.notification.application.dto.NotificationPreferenceUpdateRequest
import com.sleekydz86.idolglow.notification.domain.NotificationType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "알림 설정", description = "알림 수신 설정 API")
@RestController
@RequestMapping("/notification-preferences")
class NotificationPreferenceController(
    private val notificationPreferenceService: NotificationPreferenceService,
) {

    @Operation(summary = "알림 설정 전체 조회", description = "모든 알림 타입의 수신 설정을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        content = [Content(array = ArraySchema(schema = Schema(implementation = NotificationPreferenceResponse::class)))]
    )
    @GetMapping
    fun findAll(@Parameter(hidden = true) @LoginUser userId: Long): ResponseEntity<List<NotificationPreferenceResponse>> =
        ResponseEntity.ok(notificationPreferenceService.findAll(userId))

    @Operation(summary = "알림 설정 변경", description = "특정 알림 타입의 수신 여부를 변경합니다.")
    @ApiResponse(
        responseCode = "200",
        content = [Content(schema = Schema(implementation = NotificationPreferenceResponse::class))]
    )
    @PutMapping("/{type}")
    fun update(
        @Parameter(hidden = true) @LoginUser userId: Long,
        @Parameter(description = "알림 타입", example = "PAYMENT_FAILED") @PathVariable type: NotificationType,
        @RequestBody request: NotificationPreferenceUpdateRequest,
    ): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(notificationPreferenceService.update(userId, type, request.enabled))
}
