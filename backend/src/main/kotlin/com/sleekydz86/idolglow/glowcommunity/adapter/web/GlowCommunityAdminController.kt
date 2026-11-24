package com.sleekydz86.idolglow.glowcommunity.adapter.web

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.adapter.web.request.GlowCommunityBlindRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityReportedPostResult
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityAdminUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 GLOW 지역사회", description = "신고된 게시글 조회와 블라인드·해제 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/glow-community", "/admin/glow-community")
class GlowCommunityAdminController(
    private val glowCommunityAdminUseCase: GlowCommunityAdminUseCase,
) {
    @Operation(summary = "신고된 게시글 목록", description = "최근 신고 순. page 는 0부터 시작합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/reports")
    fun findReports(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<GlowCommunityReportedPostResult> =
        glowCommunityAdminUseCase.findReportedPosts(OffsetPageRequest.of(page, size))

    @Operation(summary = "게시글 블라인드")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postId}/blind")
    fun blind(
        @PathVariable postId: Long,
        @Valid @RequestBody(required = false) request: GlowCommunityBlindRequest?,
    ): GlowCommunityReportedPostResult = glowCommunityAdminUseCase.blind(postId, request?.reason)

    @Operation(summary = "게시글 블라인드 해제")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postId}/unblind")
    fun unblind(
        @PathVariable postId: Long,
    ): GlowCommunityReportedPostResult = glowCommunityAdminUseCase.unblind(postId)
}
