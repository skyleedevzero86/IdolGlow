package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "관리자 구독·발송",
    description = "이메일 구독자 및 소식지·웹진 발송 이력 조회 API",
)
@RestController
@RequestMapping("/admin/subscriptions")
class AdminSubscriptionController(
    private val subscriptionAdminUseCase: SubscriptionAdminUseCase,
) {

    @Operation(
        summary = "구독·발송 개요 조회",
        description = "구독자 통계와 최근 발송 이력을 관리자 구독 화면용으로 반환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun overview(
        @RequestParam(defaultValue = "1") subscriberPage: Int,
        @RequestParam(defaultValue = "10") subscriberSize: Int,
        @RequestParam(defaultValue = "1") dispatchPage: Int,
        @RequestParam(defaultValue = "10") dispatchSize: Int,
    ): ResponseEntity<AdminSubscriptionOverviewResponse> =
        ResponseEntity.ok(
            subscriptionAdminUseCase.findOverview(
                subscriberPage = subscriberPage,
                subscriberSize = subscriberSize,
                dispatchPage = dispatchPage,
                dispatchSize = dispatchSize,
            )
        )
}
