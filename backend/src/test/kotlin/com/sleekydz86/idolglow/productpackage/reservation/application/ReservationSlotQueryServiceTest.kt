package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.GlowPassBenefitPolicy
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("회원용 예약 슬롯 조회")
class ReservationSlotQueryServiceTest {
    private val slotRepository = mockk<ReservationSlotRepository>()
    private val reservationCommandService = mockk<ReservationCommandService>()
    private val policy =
        GlowPassBenefitPolicy(
            membershipQuery = mockk(),
            publicOpenDaysBefore = 7,
            earlyBookingHours = 24,
            freeCancelDaysMember = 1,
            freeCancelDaysNormal = 3,
        )
    private val service = ReservationSlotQueryService(slotRepository, reservationCommandService, policy)
    private val now = LocalDateTime.of(2026, 12, 1, 10, 0)

    @DisplayName("R9 시작 시각이 지금과 같은 슬롯은 빼고 1시간 뒤 슬롯부터 회원 가격과 오픈 시각을 함께 준다")
    @Test
    fun excludesStartedSlots() {
        // given
        val started = slot(1L, now)
        val next = slot(2L, now.plusHours(1))
        every { slotRepository.findAllByProductId(PRODUCT_ID) } returns listOf(started, next)
        every { reservationCommandService.expectedTotalPrice(PRODUCT_ID, USER_ID, now) } returns BigDecimal("44100.00")

        // when
        val slots = service.upcomingSlots(PRODUCT_ID, USER_ID, now)

        // then
        assertThat(slots.map { it.reservationSlotId }).containsExactly(2L)
        assertThat(slots.single().totalPrice).isEqualByComparingTo("44100")
        assertThat(slots.single().publicOpenAt).isEqualTo(now.plusHours(1).minusDays(7))
        assertThat(slots.single().memberOpenAt).isEqualTo(now.plusHours(1).minusDays(7).minusHours(24))
    }

    @DisplayName("R9 예약된 슬롯과 결제 중인 슬롯은 고를 수 없고, 선점이 끝난 슬롯은 고를 수 있다")
    @Test
    fun availability() {
        // given
        val booked =
            slot(1L, now.plusDays(1)).also {
                it.hold(97L, now.plusMinutes(5), now)
                it.confirmBooking(97L, now)
            }
        val held = slot(2L, now.plusDays(1).plusHours(1)).also { it.hold(99L, now.plusMinutes(5), now) }
        val expired =
            slot(3L, now.plusDays(1).plusHours(2)).also { it.hold(98L, now.plusSeconds(1), now.minusMinutes(1)) }
        val open = slot(4L, now.plusDays(1).plusHours(3))
        every { slotRepository.findAllByProductId(PRODUCT_ID) } returns listOf(booked, held, expired, open)
        every { reservationCommandService.expectedTotalPrice(PRODUCT_ID, USER_ID, any()) } returns
            BigDecimal("49000.00")

        // when
        val slots = service.upcomingSlots(PRODUCT_ID, USER_ID, now.plusSeconds(1))

        // then
        assertThat(slots.map { it.reservationSlotId to it.available })
            .containsExactly(1L to false, 2L to false, 3L to true, 4L to true)
    }

    private fun slot(
        id: Long,
        startAt: LocalDateTime,
    ): ReservationSlot {
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns PRODUCT_ID
        return ReservationSlot(
            id = id,
            product = product,
            reservationDate = startAt.toLocalDate(),
            startTime = startAt.toLocalTime(),
            endTime = startAt.toLocalTime().plusHours(1),
        )
    }

    private companion object {
        const val PRODUCT_ID = 501L
        const val USER_ID = 7L
    }
}
