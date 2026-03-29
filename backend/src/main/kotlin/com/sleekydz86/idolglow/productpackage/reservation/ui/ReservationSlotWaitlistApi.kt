package com.sleekydz86.idolglow.productpackage.reservation.ui

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.SlotWaitlistEntryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Reservation slot waitlist", description = "매진 슬롯 빈자리 알림(웨이팅)")
interface ReservationSlotWaitlistApi {

    @Operation(
        summary = "슬롯 웨이팅 등록",
        description = "해당 슬롯이 예약 확정이거나 결제 대기(선점) 중일 때만 등록할 수 있습니다. 빈자리가 나면 알림을 보냅니다."
    )
    @ApiResponse(responseCode = "201", description = "등록 성공")
    fun register(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "상품 ID", example = "1") productId: Long,
        @Parameter(description = "예약 슬롯 ID", example = "5") reservationSlotId: Long,
    ): SlotWaitlistEntryResponse

    @Operation(summary = "슬롯 웨이팅 해제", description = "등록을 취소합니다.")
    @ApiResponse(responseCode = "204", description = "해제 성공")
    fun unregister(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "상품 ID", example = "1") productId: Long,
        @Parameter(description = "예약 슬롯 ID", example = "5") reservationSlotId: Long,
    )

    @Operation(summary = "내 슬롯 웨이팅 목록", description = "등록한 슬롯 웨이팅을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findMine(
        @Parameter(hidden = true) userId: Long,
    ): List<SlotWaitlistEntryResponse>
}
