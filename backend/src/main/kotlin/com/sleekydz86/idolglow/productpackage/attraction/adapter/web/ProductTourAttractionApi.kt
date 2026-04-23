package com.sleekydz86.idolglow.productpackage.attraction.ui

import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "[사용자 전용] 상품 관광지 추천", description = "상품 기준 주변/연계 관광지 추천 API")
interface ProductTourAttractionApi {
    @Operation(
        summary = "상품 주변 관광지 추천 조회",
        description = "상품 위치(서울 자치구)를 기준으로 TourAPI를 조회해 추천 관광지를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "상품 없음", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "502", description = "외부 관광 API 오류", content = [Content(schema = Schema(hidden = true))]),
        ]
    )
    fun findTourAttractions(
        @Parameter(description = "상품 ID", example = "7")
        productId: Long,
        @Parameter(description = "조회 개수(1~30). 기본값 10", example = "10")
        size: Int?,
        @Parameter(description = "기준연월(YYYYMM). 미지정 시 전월", example = "202503")
        baseYm: String?,
        @Parameter(description = "카테고리 필터(대/중분류 부분일치)", example = "쇼핑")
        category: String?,
    ): ResponseEntity<ProductTourAttractionResponse>
}
