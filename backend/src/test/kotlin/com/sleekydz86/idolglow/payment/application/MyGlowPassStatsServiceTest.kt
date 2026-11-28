package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("내 Glow Pass 이용 통계 계산")
class MyGlowPassStatsServiceTest {
    private val chargeRepository = mockk<TossBillingChargeJpaRepository>()
    private val paymentRepository = mockk<PaymentJpaRepository>()
    private val policy =
        GlowPassBenefitPolicy(
            membershipQuery = mockk(),
            publicOpenDaysBefore = 7,
            earlyBookingHours = 24,
            freeCancelDaysMember = 1,
            freeCancelDaysNormal = 3,
        )
    private val service = MyGlowPassStatsService(chargeRepository, paymentRepository, policy)
    private val visitDate = LocalDate.of(2026, 12, 20)
    private val visitAt = LocalDateTime.of(visitDate, LocalTime.of(10, 0))
    private val publicOpenAt = visitAt.minusDays(7)

    @BeforeEach
    fun setUp() {
        every { chargeRepository.findAllByUserIdOrderByIdDesc(USER_ID) } returns emptyList()
        every { paymentRepository.findWithReservationByUserIdAndStatusIn(USER_ID, any()) } returns emptyList()
    }

    @DisplayName("M2·M3 누적 결제와 시작일은 DONE 회차만 쓰고 취소·실패 회차는 뺀다")
    @Test
    fun paidUsesDoneChargesOnly() {
        // given
        every { chargeRepository.findAllByUserIdOrderByIdDesc(USER_ID) } returns
            listOf(
                charge(13_900L, TossBillingChargeStatus.DONE, LocalDateTime.of(2026, 9, 1, 10, 0)),
                charge(4_900L, TossBillingChargeStatus.CANCELED, LocalDateTime.of(2026, 5, 3, 10, 0)),
                charge(4_900L, TossBillingChargeStatus.FAILED, null),
                charge(13_900L, TossBillingChargeStatus.DONE, LocalDateTime.of(2026, 6, 1, 10, 0)),
            )

        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.totalPaid).isEqualTo(27_800L)
        assertThat(stats.paidCount).isEqualTo(2)
        assertThat(stats.membershipSince).isEqualTo(LocalDate.of(2026, 6, 1))
    }

    @DisplayName("M3 결제 완료 회차가 없으면 시작일은 비어 있다")
    @Test
    fun noDoneChargeMeansNoStartDate() {
        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.membershipSince).isNull()
        assertThat(stats.totalPaid).isZero()
    }

    @DisplayName("M4 아낀 금액은 결제 완료된 BOOKED·COMPLETED 예약 할인만 더하고 환불 예약은 뺀다")
    @Test
    fun savedSumsBookedDiscount() {
        // given
        givenPayments(
            booked(discount = "1000.00", status = ReservationStatus.BOOKED),
            booked(discount = "1500.00", status = ReservationStatus.COMPLETED),
            refunded(discount = "9000.00", canceledAt = visitAt.minusDays(2)),
        )

        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.savedByDiscount).isEqualTo(2_500L)
    }

    @DisplayName("M5 일반 오픈 1초 전에 확정한 예약은 선예약이고, 오픈 시각에 확정한 예약은 아니다")
    @Test
    fun earlyBookingBoundary() {
        // given
        givenPayments(
            booked(confirmedAt = publicOpenAt.minusSeconds(1)),
            booked(confirmedAt = publicOpenAt),
        )

        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.earlyBookings).isEqualTo(1)
    }

    @DisplayName("M6 회원 혜택 무료 취소 구간은 방문 3일 전 제외, 방문 1일 전 포함이다")
    @Test
    fun freeCancelBoundary() {
        // given
        givenPayments(
            refunded(canceledAt = visitAt.minusDays(3)),
            refunded(canceledAt = visitAt.minusDays(3).plusSeconds(1)),
            refunded(canceledAt = visitAt.minusDays(1)),
            refunded(canceledAt = visitAt.minusDays(1).plusSeconds(1)),
        )

        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.freeCancels).isEqualTo(2)
    }

    @DisplayName("M6 운영자가 취소한 예약은 무료 취소로 세지 않는다")
    @Test
    fun adminCancelNotCounted() {
        // given
        givenPayments(refunded(canceledAt = visitAt.minusDays(2), reason = ReservationCancelReason.ADMIN_CANCELED))

        // when
        val stats = service.stats(USER_ID)

        // then
        assertThat(stats.freeCancels).isZero()
    }

    private fun givenPayments(vararg payments: Payment) {
        every { paymentRepository.findWithReservationByUserIdAndStatusIn(USER_ID, any()) } returns payments.toList()
    }

    private fun booked(
        discount: String = "0.00",
        status: ReservationStatus = ReservationStatus.BOOKED,
        confirmedAt: LocalDateTime = publicOpenAt.plusDays(1),
    ): Payment {
        val reservation = reservation(discount)
        reservation.status = status
        reservation.confirmedAt = confirmedAt
        return payment(reservation, PaymentStatus.SUCCEEDED)
    }

    private fun refunded(
        discount: String = "0.00",
        canceledAt: LocalDateTime,
        reason: ReservationCancelReason = ReservationCancelReason.USER_REQUESTED,
    ): Payment {
        val reservation = reservation(discount)
        reservation.status = ReservationStatus.CANCELED
        reservation.cancelReason = reason
        reservation.canceledAt = canceledAt
        return payment(reservation, PaymentStatus.REFUNDED)
    }

    private fun payment(
        reservation: Reservation,
        status: PaymentStatus,
    ): Payment {
        val payment = Payment.createMock(reservation, "pay_mock_${System.nanoTime()}")
        payment.status = status
        return payment
    }

    private fun reservation(discount: String): Reservation {
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns 1L
        val slot =
            ReservationSlot(
                product = product,
                reservationDate = visitDate,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
            )
        return Reservation(
            reservationSlot = slot,
            userId = USER_ID,
            visitDate = visitDate,
            visitStartTime = LocalTime.of(10, 0),
            visitEndTime = LocalTime.of(11, 0),
            totalPrice = BigDecimal("9000.00"),
            membershipDiscount = BigDecimal(discount),
        )
    }

    private fun charge(
        amount: Long,
        status: TossBillingChargeStatus,
        approvedAt: LocalDateTime?,
    ) = TossBillingCharge(
        userId = USER_ID,
        billingMethod = TossBillingMethod(userId = USER_ID, customerKey = "c", billingKey = "bk"),
        orderId = "o-${System.nanoTime()}",
        orderName = "Glow Pass",
        amount = amount,
        status = status,
        approvedAt = approvedAt,
    )

    private companion object {
        const val USER_ID = 7L
    }
}
