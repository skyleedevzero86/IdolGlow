package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.reservation.ReservationExceptionType
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

    @DisplayName("T6 다른 예약이 선점 중이면 hold 를 RESERVATION_SLOT_HELD로 거부한다")
    @Test
    fun rejectsHoldWhenSlotHeldByOther() {
        // given
        val slot = sampleSlot()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)

        // when
        val error =
            assertThrows(CustomException::class.java) {
                slot.hold(reservationId = 2L, expiresAt = expiresAt)
            }

        // then
        assertEquals(ReservationExceptionType.SLOT_HELD, error.getExceptionType())
        assertEquals(1L, slot.holdReservationId)
    }

    @DisplayName("T6 예약이 확정된 슬롯은 hold 를 RESERVATION_SLOT_ALREADY_BOOKED로 거부한다")
    @Test
    fun rejectsHoldWhenSlotBooked() {
        // given
        val slot = sampleSlot()
        slot.hold(reservationId = 1L, expiresAt = LocalDateTime.now().plusMinutes(15))
        slot.confirmBooking(reservationId = 1L)

        // when
        val error =
            assertThrows(CustomException::class.java) {
                slot.hold(reservationId = 2L, expiresAt = LocalDateTime.now().plusMinutes(15))
            }

        // then
        assertEquals(ReservationExceptionType.SLOT_ALREADY_BOOKED, error.getExceptionType())
        assertTrue(slot.isStatusBooked)
    }

    @DisplayName("T7 예약 확정이면서 선점 정보가 남은 슬롯은 RESERVATION_SLOT_ALREADY_BOOKED로 거부한다")
    @Test
    fun bookedCheckComesBeforeHeldCheck() {
        // given
        val now = LocalDateTime.now()
        val state = ReservationSlotState(PRODUCT_ID, true, 1L, now.plusMinutes(15))

        // when
        val error = assertThrows(CustomException::class.java) { state.validateAvailability(PRODUCT_ID, now) }

        // then
        assertEquals(ReservationExceptionType.SLOT_ALREADY_BOOKED, error.getExceptionType())
    }

    @DisplayName("T7 다른 예약이 만료 전 선점 중이면 RESERVATION_SLOT_HELD로 거부한다")
    @Test
    fun heldSlotIsRejected() {
        // given
        val now = LocalDateTime.now()
        val state = ReservationSlotState(PRODUCT_ID, false, 1L, now.plusMinutes(15))

        // when
        val error = assertThrows(CustomException::class.java) { state.validateAvailability(PRODUCT_ID, now) }

        // then
        assertEquals(ReservationExceptionType.SLOT_HELD, error.getExceptionType())
    }

    @DisplayName("T7 선점 만료 시각이 지금과 같으면 선점 중으로 보지 않는다")
    @Test
    fun holdExpiringNowIsNotHeld() {
        // given
        val now = LocalDateTime.now()
        val state = ReservationSlotState(PRODUCT_ID, false, 1L, now)

        // when
        state.validateAvailability(PRODUCT_ID, now)

        // then
        assertEquals(1L, state.holdReservationId)
    }

    @DisplayName("T7 다른 상품의 슬롯이면 충돌이 아니라 IllegalArgumentException으로 거부한다")
    @Test
    fun otherProductIsBadRequest() {
        // given
        val state = ReservationSlotState(PRODUCT_ID, false, null, null)

        // when
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                state.validateAvailability(PRODUCT_ID + 1, LocalDateTime.now())
            }

        // then
        assertTrue(error.message!!.contains("속하지 않습니다"))
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

    companion object {
        private const val PRODUCT_ID: Long = 10L
    }
}
