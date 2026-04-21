package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.application.AdminServerStatusService
import com.sleekydz86.idolglow.admin.ui.dto.AdminServerStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 서버 상태", description = "메트릭·인프라 헬스를 위한 관리자 대시보드 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/server-status", "/api/admin/server-status")
class AdminServerStatusController(
    private val adminServerStatusService: AdminServerStatusService,
) {

    @Operation(summary = "서버 상태 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun getStatus(): ResponseEntity<AdminServerStatusResponse> =
        ResponseEntity.ok(adminServerStatusService.getServerStatus())
}
