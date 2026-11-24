package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.reservation.ReservationExceptionType
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
        if (isBooked) throw CustomException(ReservationExceptionType.SLOT_ALREADY_BOOKED)
        require(this.productId == productId) { "해당 슬롯은 상품 ID $productId 에 속하지 않습니다." }
        if (isHeld(now)) throw CustomException(ReservationExceptionType.SLOT_HELD)
    }

    private fun isHeld(now: LocalDateTime): Boolean = holdReservationId != null && holdExpiresAt?.isAfter(now) == true
}
