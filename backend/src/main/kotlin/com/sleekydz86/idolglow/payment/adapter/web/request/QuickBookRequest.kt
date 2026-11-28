package com.sleekydz86.idolglow.payment.adapter.web.request

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

@Schema(description = "기본 카드 바로 예약 요청")
data class QuickBookRequest(
    @field:Positive
    @field:Schema(description = "상품 번호", example = "501")
    val productId: Long,
    @field:Positive
    @field:Schema(description = "예약 슬롯 번호", example = "9001")
    val reservationSlotId: Long,
    @field:DecimalMin("0")
    @field:Schema(description = "화면에 보인 결제 금액(원)", example = "44100")
    val totalPrice: BigDecimal,
) {
    fun toCommand(userId: Long): CreateReservationCommand =
        CreateReservationCommand(
            productId = productId,
            reservationSlotId = reservationSlotId,
            userId = userId,
            totalPrice = totalPrice,
            bookingChannel = ReservationBookingChannel.QUICK_BOOK,
        )
}
