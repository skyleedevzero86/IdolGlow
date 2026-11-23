package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("예약 슬롯")
class ReservationSlotDomainTest {
    @DisplayName("선점 후 동일 예약이면 hold 가 유지된다")
    @Test
    fun keepsHoldForSameReservation() {
        // given
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)

        // when
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        // then
        assertTrue(isCurrentlyHeld(slot))
    }

    @DisplayName("다른 예약이 선점 중이면 hold 를 거부한다")
    @Test
    fun rejectsHoldWhenSlotHeldByOther() {
        // given
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        // when
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        // then
        assertThrows(IllegalStateException::class.java) {
            slot.hold(reservationId = 2L, expiresAt = expiresAt)
        }
    }

    @DisplayName("선점 후 결제 확정 시 슬롯이 예약 완료된다")
    @Test
    fun booksSlotOnPaymentConfirm() {
        // given
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        // when
        slot.confirmBooking(reservationId = 1L)

        // then
        assertTrue(slot.isStatusBooked)
        assertFalse(isCurrentlyHeld(slot))
    }

    @DisplayName("선점 해제 후 슬롯을 다시 선점할 수 있다")
    @Test
    fun allowsHoldAfterRelease() {
        // given
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        slot.releaseHold(reservationId = 1L)
        // when
        slot.hold(reservationId = 2L, expiresAt = expiresAt)

        // then
        assertTrue(isCurrentlyHeld(slot))
    }

    @DisplayName("만료된 예약을 정리할 때 다른 예약이 슬롯을 잡고 있으면 예외 없이 그 잡음을 유지한다")
    @Test
    fun keepsOtherHoldWhenReleasingExpiredReservation() {
        // given
        val slot = sampleSlot()
        slot.hold(reservationId = 1L, expiresAt = LocalDateTime.now().minusMinutes(1))
        slot.hold(reservationId = 2L, expiresAt = LocalDateTime.now().plusMinutes(15))

        // when
        slot.releaseHold(reservationId = 1L)

        // then
        assertEquals(2L, slot.holdReservationId)
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
