package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductCreatedResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductBrowseResult
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import com.sleekydz86.idolglow.productpackage.product.ui.request.CreateProductRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "상품", description = "상품 조회 및 생성 관련 API")
interface ProductApi {

    @Operation(
        summary = "상품 목록 조회",
        description = "키워드·태그·가격·예약 가능일·위치 반경·정렬 등으로 상품을 탐색합니다. 최신순은 lastId 커서, 그 외 정렬은 offset으로 페이지네이션합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findProducts(
        @Parameter(description = "이전 페이지 마지막 상품 ID(최신순 전용)", example = "10")
        lastId: Long?,
        @Parameter(description = "페이지 오프셋(최신순이 아닐 때, 최대 500)", example = "0")
        offset: Int?,
        @Parameter(description = "조회 개수(기본 20, 최대 50)", example = "20")
        size: Int?,
        @Parameter(description = "단일 태그 필터(다중 태그와 함께 쓰이면 모두 만족해야 함)", example = "DREAMY")
        tag: String?,
        @Parameter(description = "추가 태그(AND). 예: tags=DREAMY&tags=성수")
        tags: List<String>?,
        @Parameter(description = "상품명·설명·태그 키워드 부분 일치")
        keyword: String?,
        @Parameter(description = "최소 가격(옵션 최저가 기준)")
        minPrice: String?,
        @Parameter(description = "최대 가격(옵션 최저가 기준)")
        maxPrice: String?,
        @Parameter(description = "방문 가능일(해당 일자에 예약 가능한 슬롯이 있는 상품)", example = "2026-04-02")
        visitDate: String?,
        @Parameter(description = "오늘 이후 예약 가능 슬롯이 하나라도 있는 상품만")
        reservableOnly: Boolean?,
        @Parameter(description = "정렬: NEWEST, POPULARITY, RATING, REVIEW_COUNT, DISTANCE")
        sort: String?,
        @Parameter(description = "기준점 위도(거리 정렬·반경 필터)")
        nearLatitude: String?,
        @Parameter(description = "기준점 경도")
        nearLongitude: String?,
        @Parameter(description = "반경(미터). 위경도가 있을 때만 적용, 최대 200000")
        radiusMeters: Int?,
    ): ResponseEntity<ProductBrowseResult>

    @Operation(
        summary = "상품 상세 조회",
        description = "특정 상품의 상세 정보와 포함 옵션, 예약 가능 범위를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "상품을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun findProduct(
        @Parameter(description = "상품 ID", example = "1")
        productId: Long
    ): ResponseEntity<ProductSpecificResponse>

    @Operation(
        summary = "상품 생성",
        description = "관리자가 상품과 기본 예약 슬롯 범위를 생성합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun createProduct(
        @Parameter(description = "상품 생성 요청 본문")
        @Valid request: CreateProductRequest
    ): ResponseEntity<ProductCreatedResponse>
}
