package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ReservationSlotDomainTest {
    @Test
    fun `선점 후 동일 예약이면 hold 가 유지된다`() {
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)

        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        assertTrue(isCurrentlyHeld(slot))
    }

    @Test
    fun `다른 예약이 선점 중이면 hold 를 거부한다`() {
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        assertThrows(IllegalStateException::class.java) {
            slot.hold(reservationId = 2L, expiresAt = expiresAt)
        }
    }

    @Test
    fun `선점 후 결제 확정 시 슬롯이 예약 완료된다`() {
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        slot.confirmBooking(reservationId = 1L)

        assertTrue(slot.isStatusBooked)
        assertFalse(isCurrentlyHeld(slot))
    }

    @Test
    fun `선점 해제 후 슬롯을 다시 선점할 수 있다`() {
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        slot.releaseHold(reservationId = 1L)
        slot.hold(reservationId = 2L, expiresAt = expiresAt)

        assertTrue(isCurrentlyHeld(slot))
    }

    private fun isCurrentlyHeld(slot: ReservationSlot): Boolean {
        val now = LocalDateTime.now()
        return slot.holdReservationId != null && slot.holdExpiresAt?.isAfter(now) == true
    }

    private fun sampleSlot(): ReservationSlot {
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
        return product.reservationSlots.first()
    }
}
