package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.payment.application.AdminBillingStatsService
import com.sleekydz86.idolglow.payment.application.dto.AdminBillingStatsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 Glow Pass 통계", description = "관리자 Glow Pass 구독 통계 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/billing")
@PreAuthorize("hasRole('ADMIN')")
class AdminBillingStatsController(
    private val adminBillingStatsService: AdminBillingStatsService,
) {
    @Operation(summary = "Glow Pass 구독 통계 조회")
    @GetMapping("/stats")
    fun stats(): ResponseEntity<AdminBillingStatsResponse> = ResponseEntity.ok(adminBillingStatsService.stats())
}
