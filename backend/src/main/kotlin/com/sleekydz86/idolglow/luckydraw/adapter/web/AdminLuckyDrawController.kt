package com.sleekydz86.idolglow.luckydraw.adapter.web

import com.sleekydz86.idolglow.luckydraw.adapter.web.request.LuckyDrawRequest
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawAdminService
import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawAdminResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "응모 관리", description = "관리자 응모 행사 등록·수정·삭제·조회 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/lucky-draws")
class AdminLuckyDrawController(
    private val luckyDrawAdminService: LuckyDrawAdminService,
) {
    @Operation(summary = "응모 행사 목록")
    @GetMapping
    fun list(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<List<LuckyDrawAdminResponse>> = ResponseEntity.ok(luckyDrawAdminService.list(page, size))

    @Operation(summary = "응모 행사 상세", description = "응모자 수, 응모 수, 가산 결제 수, 당첨자를 함께 돌려줍니다.")
    @GetMapping("/{drawId}")
    fun detail(
        @PathVariable drawId: Long,
    ): ResponseEntity<LuckyDrawAdminResponse> = ResponseEntity.ok(luckyDrawAdminService.detail(drawId))

    @Operation(summary = "응모 행사 등록", description = "응모 기간이 겹치면 409입니다.")
    @PostMapping
    fun create(
        @RequestBody request: LuckyDrawRequest,
    ): ResponseEntity<LuckyDrawAdminResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(luckyDrawAdminService.create(request.toDraft()))

    @Operation(summary = "응모 행사 수정", description = "응모가 시작된 행사는 409입니다.")
    @PutMapping("/{drawId}")
    fun update(
        @PathVariable drawId: Long,
        @RequestBody request: LuckyDrawRequest,
    ): ResponseEntity<LuckyDrawAdminResponse> =
        ResponseEntity.ok(luckyDrawAdminService.update(drawId, request.toDraft()))

    @Operation(summary = "응모 행사 삭제", description = "응모가 시작된 행사는 409입니다.")
    @DeleteMapping("/{drawId}")
    fun delete(
        @PathVariable drawId: Long,
    ): ResponseEntity<Void> {
        luckyDrawAdminService.delete(drawId)
        return ResponseEntity.noContent().build()
    }
}
