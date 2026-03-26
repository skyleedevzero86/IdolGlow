package com.sleekydz86.idolglow.review.ui

import com.sleekydz86.idolglow.review.application.dto.ProductReviewResponse
import com.sleekydz86.idolglow.review.ui.request.CreateProductReviewRequest
import com.sleekydz86.idolglow.review.ui.request.UpdateProductReviewRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Product Review", description = "상품 리뷰 관리 API")
interface ProductReviewApi {

    @Operation(summary = "상품 리뷰 목록 조회", description = "상품의 모든 리뷰를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findReviews(
        @Parameter(description = "상품 ID", example = "1")
        productId: Long
    ): List<ProductReviewResponse>

    @Operation(summary = "상품 리뷰 생성", description = "로그인한 사용자가 리뷰를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청")
        ]
    )
    fun createReview(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "리뷰 생성 요청")
        request: CreateProductReviewRequest,
        @Parameter(
            description = "리뷰 이미지 파일 목록",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = ArraySchema(schema = Schema(type = "string", format = "binary"))
                )
            ]
        )
        images: List<MultipartFile>?
    ): ProductReviewResponse

    @Operation(summary = "상품 리뷰 수정", description = "로그인한 사용자가 자신의 리뷰를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
        ]
    )
    fun updateReview(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "리뷰 ID", example = "10")
        reviewId: Long,
        @Parameter(description = "리뷰 수정 요청")
        request: UpdateProductReviewRequest,
        @Parameter(
            description = "리뷰 이미지 파일 목록",
            content = [
                Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    array = ArraySchema(schema = Schema(type = "string", format = "binary"))
                )
            ]
        )
        images: List<MultipartFile>?
    ): ProductReviewResponse

    @Operation(summary = "상품 리뷰 삭제", description = "로그인한 사용자가 자신의 리뷰를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
        ]
    )
    fun deleteReview(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "리뷰 ID", example = "10")
        reviewId: Long
    )
}
