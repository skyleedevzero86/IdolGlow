package com.sleekydz86.idolglow.glowalert.adapter.web

import com.sleekydz86.idolglow.glowalert.application.GlowAlertQueryService
import com.sleekydz86.idolglow.glowalert.application.dto.GlowAlertPageResponse
import com.sleekydz86.idolglow.glowalert.application.dto.GlowAlertUnreadCountResponse
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 알림", description = "Glow 알림 목록 조회 API")
@Validated
@RestController
class GlowAlertController(
    private val glowAlertQueryService: GlowAlertQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @Operation(summary = "Glow 알림 목록 조회")
    @GetMapping("/api/glow-alerts", "/glow-alerts", "/mypage/glow-alerts")
    fun findAlerts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "8") size: Int,
        @RequestParam(defaultValue = "unread") status: String,
        @RequestParam(defaultValue = "all") category: String,
        @RequestParam(defaultValue = "") q: String,
    ): ResponseEntity<GlowAlertPageResponse> =
        ResponseEntity.ok(
            glowAlertQueryService.findAlerts(
                page = page,
                size = size,
                status = status,
                category = category,
                keyword = q,
                userId = authenticatedUserIdResolver.resolveOrNull(),
            )
        )

    @Operation(summary = "읽지 않은 Glow 알림 수 조회")
    @GetMapping("/api/glow-alerts/unread-count", "/glow-alerts/unread-count", "/mypage/glow-alerts/unread-count")
    fun countUnread(): ResponseEntity<GlowAlertUnreadCountResponse> =
        ResponseEntity.ok(GlowAlertUnreadCountResponse(glowAlertQueryService.countUnread(authenticatedUserIdResolver.resolveOrNull())))

    @Operation(summary = "Glow 알림 읽음 처리")
    @PostMapping("/api/glow-alerts/{alertId}/read", "/glow-alerts/{alertId}/read", "/mypage/glow-alerts/{alertId}/read")
    fun markRead(
        @PathVariable alertId: Long,
    ): ResponseEntity<GlowAlertUnreadCountResponse> {
        val userId = authenticatedUserIdResolver.resolveOrNull()
        glowAlertQueryService.markRead(alertId, userId)
        return ResponseEntity.ok(GlowAlertUnreadCountResponse(glowAlertQueryService.countUnread(userId)))
    }
}
