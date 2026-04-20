package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.application.AdminUserService
import com.sleekydz86.idolglow.admin.ui.dto.AdminUserPageResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminUserSummaryResponse
import com.sleekydz86.idolglow.admin.ui.request.UpdateAdminUserRoleRequest
import com.sleekydz86.idolglow.admin.ui.request.UpdateAdminUserStatusRequest
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 회원관리", description = "관리자 회원 목록 조회 및 상태 변경 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/users", "/api/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService,
) {

    @Operation(summary = "회원 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun findUsers(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) accountStatus: UserAccountStatus?,
    ): ResponseEntity<AdminUserPageResponse> =
        ResponseEntity.ok(adminUserService.findUsers(keyword, role, accountStatus, page, size))

    @Operation(summary = "회원 역할 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{userId}/role")
    fun updateRole(
        @PathVariable userId: Long,
        @RequestBody request: UpdateAdminUserRoleRequest,
    ): ResponseEntity<AdminUserSummaryResponse> =
        ResponseEntity.ok(adminUserService.updateUserRole(userId, request.role))

    @Operation(summary = "회원 상태 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{userId}/status")
    fun updateStatus(
        @PathVariable userId: Long,
        @RequestBody request: UpdateAdminUserStatusRequest,
    ): ResponseEntity<AdminUserSummaryResponse> =
        ResponseEntity.ok(adminUserService.updateUserStatus(userId, request.accountStatus))

    @Operation(summary = "회원 잠금 해제")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/unlock")
    fun unlock(
        @PathVariable userId: Long,
    ): ResponseEntity<AdminUserSummaryResponse> =
        ResponseEntity.ok(adminUserService.unlockUser(userId))
}
