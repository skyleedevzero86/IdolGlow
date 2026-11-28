package com.sleekydz86.idolglow.productpackage.reservation.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotQueryService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.OpenReservationSlotResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Tag(name = "예약 슬롯 조회", description = "회원이 고를 수 있는 다가오는 예약 슬롯")
@RestController
class ReservationSlotQueryController(
    private val reservationSlotQueryService: ReservationSlotQueryService,
) {
    @Operation(summary = "상품의 다가오는 예약 슬롯 조회 (최대 30개)")
    @GetMapping("/products/{productId}/reservation-slots")
    fun upcomingSlots(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<List<OpenReservationSlotResponse>> =
        ResponseEntity.ok(reservationSlotQueryService.upcomingSlots(productId, userId))
}
