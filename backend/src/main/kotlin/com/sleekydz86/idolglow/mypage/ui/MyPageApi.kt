package com.sleekydz86.idolglow.mypage.ui

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "MyPage", description = "마이페이지 관련 API")
interface MyPageApi {

    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 사용자 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getUser(
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<GetUserLoginInfoResponse>

    @Operation(
        summary = "예약 내역 조회",
        description = "마이페이지에서 상품 예약 내역을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findBookings(
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<List<ReservationSummaryResponse>>

    @Operation(
        summary = "다가오는 예약 조회",
        description = "마이페이지에서 다가오는 상품 예약 내역을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findUpcomingBookings(
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<List<ReservationSummaryResponse>>

    @Operation(
        summary = "예약 상세 조회",
        description = "마이페이지에서 특정 예약의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "예약을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun findBookingDetail(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "예약 ID", example = "1")
        reservationId: Long
    ): ResponseEntity<ReservationSummaryResponse>

    @Operation(
        summary = "내 리뷰 조회",
        description = "마이페이지에서 사용자가 작성한 리뷰를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findMyReviews(
        @Parameter(hidden = true)
        userId: Long
    ): ResponseEntity<List<ProductReviewResponse>>
}
