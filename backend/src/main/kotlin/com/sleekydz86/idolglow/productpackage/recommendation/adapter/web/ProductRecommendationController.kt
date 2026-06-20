package com.sleekydz86.idolglow.productpackage.recommendation.adapter.web

import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request.UpdateProductAdminRecommendationRequest
import com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request.UpdateProductRecommendationScoreRequest
import com.sleekydz86.idolglow.productpackage.recommendation.adapter.web.request.UpsertLatestKoreaRecommendationRequest
import com.sleekydz86.idolglow.productpackage.recommendation.application.ProductRecommendationService
import com.sleekydz86.idolglow.productpackage.recommendation.application.dto.LatestKoreaRecommendationUpdateResponse
import com.sleekydz86.idolglow.productpackage.recommendation.application.dto.ProductAdminRecommendationUpdateResponse
import com.sleekydz86.idolglow.productpackage.recommendation.application.dto.ProductRecommendationScoreUpdateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "상품 추천", description = "상품 추천 목록 및 관리자 추천 설정 API")
@RestController
@RequestMapping
class ProductRecommendationController(
    private val productRecommendationService: ProductRecommendationService,
) {
    @Operation(summary = "Latest in Korea 추천 일괄 교체")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/admin/products/recommendations/latest-in-korea")
    @PreAuthorize("hasRole('ADMIN')")
    fun replaceLatestInKorea(
        @Valid @RequestBody request: UpsertLatestKoreaRecommendationRequest,
    ): ResponseEntity<LatestKoreaRecommendationUpdateResponse> =
        ResponseEntity.ok(
            LatestKoreaRecommendationUpdateResponse(
                productCount = productRecommendationService.replaceLatestInKorea(request.productIds),
            ),
        )

    @Operation(summary = "Latest in Korea 추천 목록 조회")
    @GetMapping("/products/recommendations/latest-in-korea")
    fun findLatestInKorea(
        @RequestParam(required = false, defaultValue = "20") size: Int?,
        @RequestParam(required = false) tag: String?,
    ): ResponseEntity<List<ProductPagingQueryResponse>> = ResponseEntity.ok(productRecommendationService.findLatestInKorea(size ?: 20, tag))

    @Operation(summary = "관리자 추천 여부 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/admin/products/{productId}/recommendation")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateAdminRecommendation(
        @PathVariable productId: Long,
        @Valid @RequestBody request: UpdateProductAdminRecommendationRequest,
    ): ResponseEntity<ProductAdminRecommendationUpdateResponse> =
        ResponseEntity.ok(
            productRecommendationService.updateAdminRecommendation(
                productId = productId,
                recommended = request.recommended,
            ),
        )

    @Operation(summary = "추천 점수 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/admin/products/{productId}/recommendation-score")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateRecommendationScore(
        @PathVariable productId: Long,
        @Valid @RequestBody request: UpdateProductRecommendationScoreRequest,
    ): ResponseEntity<ProductRecommendationScoreUpdateResponse> =
        ResponseEntity.ok(
            productRecommendationService.updateRecommendationScore(
                productId = productId,
                score = request.score,
            ),
        )

    @Operation(summary = "관리자 픽 추천 목록 조회")
    @GetMapping("/products/recommendations/admin-picked")
    fun findAdminRecommended(
        @RequestParam(required = false, defaultValue = "20") size: Int?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) query: String?,
    ): ResponseEntity<List<ProductPagingQueryResponse>> =
        ResponseEntity.ok(
            productRecommendationService.findAdminRecommended(
                size = size ?: 20,
                tagName = tag,
                searchKeyword = query,
            ),
        )
}
