package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductCreatedResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
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

@Tag(name = "Product", description = "상품 조회 및 생성 관련 API")
interface ProductApi {

    @Operation(
        summary = "상품 목록 조회",
        description = "태그와 커서를 기준으로 상품 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findProducts(
        @Parameter(description = "이전 페이지 마지막 상품 ID", example = "10")
        lastId: Long?,
        @Parameter(description = "조회 개수(기본 20, 최대 50)", example = "20")
        size: Int?,
        @Parameter(description = "태그명 필터", example = "글로우")
        tag: String?,
    ): ResponseEntity<List<ProductPagingQueryResponse>>

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
