package com.sleekydz86.idolglow.review.ui

import com.sleekydz86.idolglow.review.application.dto.ProductReviewResponse
import com.sleekydz86.idolglow.review.application.dto.ReviewHelpfulCountResponse
import com.sleekydz86.idolglow.review.ui.request.CreateProductReviewRequest
import com.sleekydz86.idolglow.review.ui.request.ReportProductReviewRequest
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

@Tag(name = "상품 리뷰", description = "상품 리뷰 관리 API")
interface ProductReviewApi {

    @Operation(
        summary = "상품 리뷰 목록 조회",
        description = "비공개(신고 누적 등) 리뷰는 제외합니다. 사진이 있는 리뷰·도움돼요 순으로 정렬됩니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findReviews(
        @Parameter(description = "상품 ID", example = "1")
        productId: Long
    ): List<ProductReviewResponse>

    @Operation(
        summary = "상품 리뷰 생성",
        description = "해당 상품에 대해 방문이 완료된 예약(확정·과거 방문일)이 있을 때만 작성할 수 있습니다."
    )
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

    @Operation(summary = "리뷰 도움돼요 토글", description = "같은 사용자가 반복 호출 시 취소됩니다. 본인 리뷰에는 불가합니다.")
    @ApiResponse(responseCode = "200", description = "성공")
    fun toggleHelpful(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "리뷰 ID", example = "10")
        reviewId: Long,
    ): ReviewHelpfulCountResponse

    @Operation(summary = "리뷰 신고", description = "동일 사용자의 중복 신고는 불가합니다. 누적 시 자동 비공개 처리될 수 있습니다.")
    @ApiResponse(responseCode = "204", description = "접수 성공")
    fun reportReview(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "리뷰 ID", example = "10")
        reviewId: Long,
        request: ReportProductReviewRequest,
    )
}
