package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.application.AdminProductReviewService
import com.sleekydz86.idolglow.admin.application.AdminReviewVisibilityFilter
import com.sleekydz86.idolglow.admin.ui.dto.AdminProductReviewPageResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminProductReviewSummaryResponse
import com.sleekydz86.idolglow.admin.ui.request.HideAdminProductReviewRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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

@Tag(name = "관리자 상품 리뷰", description = "상품 리뷰 목록·비공개·공개 복구 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/reviews", "/api/admin/reviews")
class AdminProductReviewController(
    private val adminProductReviewService: AdminProductReviewService,
) {

    @Operation(summary = "리뷰 목록 조회", description = "키워드(내용·상품명), 공개 여부 필터, 페이지네이션")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun findReviews(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "ALL") visibility: AdminReviewVisibilityFilter,
    ): ResponseEntity<AdminProductReviewPageResponse> =
        ResponseEntity.ok(adminProductReviewService.findReviews(keyword, visibility, page, size))

    @Operation(summary = "리뷰 비공개 처리")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{reviewId}/hide")
    fun hideReview(
        @PathVariable reviewId: Long,
        @Valid @RequestBody(required = false) request: HideAdminProductReviewRequest?,
    ): ResponseEntity<AdminProductReviewSummaryResponse> =
        ResponseEntity.ok(adminProductReviewService.hideReview(reviewId, request?.reason))

    @Operation(summary = "리뷰 공개 복구")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{reviewId}/unhide")
    fun unhideReview(
        @PathVariable reviewId: Long,
    ): ResponseEntity<AdminProductReviewSummaryResponse> =
        ResponseEntity.ok(adminProductReviewService.unhideReview(reviewId))
}
