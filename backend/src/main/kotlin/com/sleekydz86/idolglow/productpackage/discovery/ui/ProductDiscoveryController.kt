package com.sleekydz86.idolglow.productpackage.discovery.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Product Discovery", description = "상품 추천 및 랭킹 조회 API")
@RestController
@RequestMapping("/products")
class ProductDiscoveryController(
    private val productDiscoveryService: ProductDiscoveryService,
) {

    @Operation(summary = "인기 상품 랭킹 조회", description = "예약과 찜 지표를 바탕으로 인기 상품 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인기 상품 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ProductRankingResponse::class)))]
            )
        ]
    )
    @GetMapping("/rankings/popular")
    fun findPopularProducts(
        @Parameter(description = "조회할 상품 개수", example = "10")
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<ProductRankingResponse>> =
        ResponseEntity.ok(productDiscoveryService.findPopularProducts(size.coerceIn(1, 50)))

    @Operation(
        summary = "개인화 추천 상품 조회",
        description = "회원 설문(컨셉·방문 기간·장소·아이돌 무드)과 찜/리뷰 기반 태그, 예약 가능 일정 적합도를 합산해 추천합니다. 설문만 있는 신규 회원도 콜드스타트 완화에 활용됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "추천 상품 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ProductRankingResponse::class)))]
            )
        ]
    )
    @GetMapping("/recommendations")
    fun findRecommendedProducts(
        @Parameter(hidden = true)
        @LoginUser userId: Long,
        @Parameter(description = "조회할 상품 개수", example = "10")
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<List<ProductRankingResponse>> =
        ResponseEntity.ok(productDiscoveryService.findRecommendedProducts(userId, size.coerceIn(1, 50)))
}
