package com.sleekydz86.idolglow.admin.authverification.ui

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.admin.authverification.ui.dto.AuthVerificationAuditLogPageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 인증로그", description = "회원가입/계정복구 인증 기록 조회 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/auth-verifications")
class AdminAuthVerificationController(
    private val authVerificationAuditService: AuthVerificationAuditService,
) {
    @Operation(summary = "인증 기록 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) verificationType: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<AuthVerificationAuditLogPageResponse> = ResponseEntity.ok(
        authVerificationAuditService.findPage(
            page = page,
            size = size,
            verificationType = verificationType,
            keyword = keyword,
        ),
    )
}
