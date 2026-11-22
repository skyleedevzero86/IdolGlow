package com.sleekydz86.idolglow.productpackage.reservation.domain

import java.time.LocalDateTime

data class ReservationSlotState(
    val productId: Long,
    val isBooked: Boolean,
    val holdReservationId: Long?,
    val holdExpiresAt: LocalDateTime?,
) {
    fun validateAvailability(
        productId: Long,
        now: LocalDateTime,
    ) {
        require(!isBooked) { "이미 예약된 슬롯입니다." }
        require(this.productId == productId) { "해당 슬롯은 상품 ID $productId 에 속하지 않습니다." }
        require(!isHeld(now)) { "다른 예약이 이미 선점한 슬롯입니다." }
    }

    private fun isHeld(now: LocalDateTime): Boolean = holdReservationId != null && holdExpiresAt?.isAfter(now) == true
}
