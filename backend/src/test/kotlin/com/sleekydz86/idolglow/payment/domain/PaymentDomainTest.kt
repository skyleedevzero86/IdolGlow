package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class PaymentDomainTest {
    @Test
    fun `대기 중인 결제는 승인될 수 있다`() {
        val payment = samplePendingPayment()

        payment.markSucceeded()

        assertEquals(PaymentStatus.SUCCEEDED, payment.status)
    }

    @Test
    fun `이미 승인된 결제는 중복 승인 호출이 무해하다`() {
        val payment = samplePendingPayment()
        payment.markSucceeded()

        payment.markSucceeded()

        assertEquals(PaymentStatus.SUCCEEDED, payment.status)
    }

    @Test
    fun `승인된 결제는 실패 처리할 수 없다`() {
        val payment = samplePendingPayment()
        payment.markSucceeded()

        assertThrows(IllegalArgumentException::class.java) {
            payment.markFailed("실패")
        }
    }

    @Test
    fun `대기 중인 결제는 만료 처리할 수 있다`() {
        val payment = samplePendingPayment()

        payment.markExpired()

        assertEquals(PaymentStatus.EXPIRED, payment.status)
    }

    private fun samplePendingPayment(): Payment {
        val visitDate = LocalDate.now().plusDays(3)
        val product =
            Product.createWithTimeSlots(
                name = "테스트 상품",
                description = "설명",
                options = emptyList(),
                tagNames = emptyList(),
                slotStartDate = visitDate,
                slotEndDate = visitDate,
            )
        val slot = product.reservationSlots.first()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)
        val reservation =
            Reservation(
                id = 1L,
                reservationSlot = slot,
                userId = 10L,
                visitDate = slot.reservationDate,
                visitStartTime = slot.startTime,
                visitEndTime = slot.endTime,
                totalPrice = BigDecimal("35000"),
            ).request(expiresAt)
        return Payment.createMock(reservation, "pay_test_ref")
    }
}
