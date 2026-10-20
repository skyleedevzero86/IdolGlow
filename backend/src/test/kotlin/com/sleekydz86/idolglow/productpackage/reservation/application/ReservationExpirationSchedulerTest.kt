package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("예약 만료 스케줄러")
class ReservationExpirationSchedulerTest {
    private val reservationCommandService = mockk<ReservationCommandService>(relaxed = true)
    private val reservationRepository = mockk<ReservationRepository>()

    private val scheduler =
        ReservationExpirationScheduler(
            reservationCommandService = reservationCommandService,
            reservationRepository = reservationRepository,
        )

    @DisplayName("만료 예약 하나가 실패해도 나머지 예약은 만료 처리한다")
    @Test
    fun continuesWhenOneExpirationFails() {
        // given
        every { reservationRepository.findExpiredPendingIds(any(), any()) } returns listOf(1L, 2L, 3L)
        every { reservationCommandService.expirePendingReservation(2L, any()) } throws
            IllegalStateException("만료 처리 실패")

        // when
        scheduler.expirePendingReservations()

        // then
        verify(exactly = 1) { reservationCommandService.expirePendingReservation(1L, any()) }
        verify(exactly = 1) { reservationCommandService.expirePendingReservation(3L, any()) }
    }

    @DisplayName("한 번 실행에 만료 예약 목록을 한 번만 조회한다")
    @Test
    fun readsExpiredReservationsOncePerRun() {
        // given
        every { reservationRepository.findExpiredPendingIds(any(), any()) } returns listOf(1L)

        // when
        scheduler.expirePendingReservations()

        // then
        verify(exactly = 1) { reservationRepository.findExpiredPendingIds(any(), any()) }
    }
}
