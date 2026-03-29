package com.sleekydz86.idolglow.productpackage.reservation.ui

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.ui.request.CreateReservationRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "Reservation", description = "상품 예약 생성 및 취소 API")
interface ReservationApi {

    @Operation(
        summary = "예약 생성",
        description = "로그인한 사용자가 상품 예약을 생성하고 결제 대기 정보를 받습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                headers = [
                    Header(
                        name = "Location",
                        description = "생성된 예약 URI",
                        schema = Schema(type = "string")
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun createReservation(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Valid request: CreateReservationRequest
    ): ResponseEntity<ReservationCreatedResponse>

    @Operation(
        summary = "예약 취소",
        description = "로그인한 사용자가 자신의 예약을 취소합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "취소 성공"),
            ApiResponse(
                responseCode = "404",
                description = "예약을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun cancelReservation(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "상품 ID", example = "1")
        productId: Long,
        @Parameter(description = "예약 ID", example = "1")
        reservationId: Long,
    ): ResponseEntity<ReservationSummaryResponse>
}
