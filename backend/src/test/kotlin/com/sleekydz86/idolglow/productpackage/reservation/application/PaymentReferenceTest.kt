package com.sleekydz86.idolglow.productpackage.reservation.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("결제 참조값")
class PaymentReferenceTest {
    private val now = LocalDateTime.of(2026, 10, 20, 21, 30, 0)

    @DisplayName("토스 결제는 pay_toss_ 로 시작한다")
    @Test
    fun tossPaymentUsesTossPrefix() {
        // given
        val now = this.now

        // when
        val reference =
            ReservationCommandService.paymentReferenceOf(
                useTossProvider = true,
                reservationId = 7L,
                now = now,
            )

        // then
        assertEquals("pay_toss_20261020213000_7", reference)
    }

    @DisplayName("목업 결제는 pay_mock_ 로 시작한다")
    @Test
    fun mockPaymentUsesMockPrefix() {
        // given
        val now = this.now

        // when
        val reference =
            ReservationCommandService.paymentReferenceOf(
                useTossProvider = false,
                reservationId = 7L,
                now = now,
            )

        // then
        assertEquals("pay_mock_20261020213000_7", reference)
    }
}
