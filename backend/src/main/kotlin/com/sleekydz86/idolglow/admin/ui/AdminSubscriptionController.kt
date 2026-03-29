package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Admin subscription",
    description = "관리자 전용 구독·과금 요약(연동 전에는 플레이스홀더 응답일 수 있음).",
)
@RestController
@RequestMapping("/admin/subscriptions")
class AdminSubscriptionController {

    @Operation(
        summary = "구독 개요",
        description = "활성 구독 수 등 관리자 대시보드용 요약을 반환합니다. **ADMIN** 역할과 유효한 Bearer JWT가 필요합니다.",
    )
    @ApiResponse(responseCode = "200", description = "개요 데이터")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun overview(): ResponseEntity<AdminSubscriptionOverviewResponse> =
        ResponseEntity.ok(AdminSubscriptionOverviewResponse())
}
