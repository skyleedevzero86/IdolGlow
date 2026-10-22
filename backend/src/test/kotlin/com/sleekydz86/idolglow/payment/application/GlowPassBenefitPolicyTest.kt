package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("글로우패스 혜택")
class GlowPassBenefitPolicyTest {
    private val membershipQuery = mockk<MembershipQuery>()
    private val policy =
        GlowPassBenefitPolicy(
            membershipQuery = membershipQuery,
            publicOpenDaysBefore = 7,
            earlyBookingHours = 24,
            freeCancelDaysMember = 1,
            freeCancelDaysNormal = 3,
        )

    @DisplayName("일반 오픈 전에는 비멤버 예약을 거절한다")
    @Test
    fun rejectsNonMemberBeforePublicOpen() {
        // given
        val slot = slot(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0))
        val now = LocalDateTime.of(2026, 11, 2, 10, 0)
        // when
        every { membershipQuery.isActive(1L, now) } returns false

        // then
        assertThrows(IllegalArgumentException::class.java) {
            policy.assertEarlyBookingAllowed(1L, slot, now)
        }
    }

    @DisplayName("멤버는 일반 오픈 24시간 전부터 예약할 수 있다")
    @Test
    fun allowsMemberBookingOneDayBeforePublicOpen() {
        // given
        val slot = slot(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0))
        val publicOpen = LocalDateTime.of(2026, 11, 3, 10, 0)
        val now = publicOpen.minusHours(12)
        // when
        every { membershipQuery.isActive(1L, now) } returns true

        // then
        assertDoesNotThrow {
            policy.assertEarlyBookingAllowed(1L, slot, now)
        }
    }

    @DisplayName("비멤버는 방문 3일 전까지만 무료 취소 가능하다")
    @Test
    fun allowsNonMemberFreeCancelUntilThreeDaysBefore() {
        // given
        val reservation = reservation(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0))
        val now = LocalDateTime.of(2026, 11, 8, 10, 0)
        // when
        every { membershipQuery.isActive(1L, now) } returns false

        // then
        assertThrows(IllegalArgumentException::class.java) {
            policy.assertFreeCancelAllowed(1L, reservation, now)
        }
    }

    @DisplayName("멤버는 방문 1일 전까지 무료 취소 가능하다")
    @Test
    fun allowsMemberFreeCancelUntilOneDayBefore() {
        // given
        val reservation = reservation(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0))
        val now = LocalDateTime.of(2026, 11, 9, 9, 0)
        // when
        every { membershipQuery.isActive(1L, now) } returns true

        // then
        assertDoesNotThrow {
            policy.assertFreeCancelAllowed(1L, reservation, now)
        }
    }

    private fun slot(
        date: LocalDate,
        start: LocalTime,
    ): ReservationSlot {
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns 1L
        return ReservationSlot(
            product = product,
            reservationDate = date,
            startTime = start,
            endTime = start.plusHours(1),
        )
    }

    private fun reservation(
        date: LocalDate,
        start: LocalTime,
    ): Reservation {
        val slot = slot(date, start)
        return Reservation(
            reservationSlot = slot,
            userId = 1L,
            visitDate = date,
            visitStartTime = start,
            visitEndTime = start.plusHours(1),
            totalPrice = BigDecimal("10000"),
        )
    }
}
