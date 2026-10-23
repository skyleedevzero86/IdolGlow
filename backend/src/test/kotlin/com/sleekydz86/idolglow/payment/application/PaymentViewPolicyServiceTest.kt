package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("결제 화면 정책")
class PaymentViewPolicyServiceTest {
    private val policy = PaymentViewPolicyService()

    @DisplayName("환불 처리 중 결제도 영수증을 받을 수 있다")
    @Test
    fun receiptAvailableIncludesRefunding() {
        // given
        val refunding = samplePayment()
        refunding.markSucceeded()
        refunding.markRefunding()
        val pending = samplePayment()

        // when
        val refundingReceipt = policy.receiptAvailable(refunding)
        val pendingReceipt = policy.receiptAvailable(pending)

        // then
        assertTrue(refundingReceipt)
        assertEquals(PaymentStatus.REFUNDING, refunding.status)
        assertFalse(pendingReceipt)
    }

    private fun samplePayment(): Payment {
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
        return Payment.createMock(reservation, "pay_receipt")
    }
}
