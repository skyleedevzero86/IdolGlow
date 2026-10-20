package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Reservation Payment Service")
class ReservationPaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val reservationCommandService = mockk<ReservationCommandService>(relaxed = true)
    private val notificationCommandService = mockk<NotificationCommandService>(relaxed = true)
    private val reservationSlotWaitlistService = mockk<ReservationSlotWaitlistService>(relaxed = true)
    private val paymentNotificationMailService = mockk<PaymentNotificationMailService>(relaxed = true)
    private val paymentRefundService = mockk<PaymentRefundService>(relaxed = true)

    private lateinit var service: ReservationPaymentService

    @BeforeEach
    fun setUp() {
        service =
            ReservationPaymentService(
                paymentRepository = paymentRepository,
                reservationRepository = reservationRepository,
                reservationCommandService = reservationCommandService,
                notificationCommandService = notificationCommandService,
                reservationSlotWaitlistService = reservationSlotWaitlistService,
                paymentNotificationMailService = paymentNotificationMailService,
                paymentRefundService = paymentRefundService,
            )
    }

    @DisplayName("결제 성공 시 예약을 확정한다")
    @Test
    fun confirmsReservationOnPaymentSuccess() {
        // given
        val fixture = pendingFixture()
        every { paymentRepository.findByPaymentReference("pay_ok") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { paymentRepository.findByReservationIdForUpdate(1L) } returns fixture.payment
        every { reservationCommandService.confirmReservation(1L) } answers {
            fixture.reservation.confirm()
            fixture.reservation
        }

        // when
        val result = service.handlePaymentSucceeded("pay_ok")

        // then
        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        verify(exactly = 1) { reservationCommandService.confirmReservation(1L) }
        verify(exactly = 1) { paymentNotificationMailService.sendSucceeded(fixture.payment) }
    }

    @DisplayName("결제 성공 처리는 예약을 먼저 잠그고 결제를 나중에 잠근다")
    @Test
    fun locksReservationBeforePaymentOnSuccess() {
        // given
        val fixture = pendingFixture()
        every { paymentRepository.findByPaymentReference("pay_ok") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { paymentRepository.findByReservationIdForUpdate(1L) } returns fixture.payment
        every { reservationCommandService.confirmReservation(1L) } answers {
            fixture.reservation.confirm()
            fixture.reservation
        }

        // when
        service.handlePaymentSucceeded("pay_ok")

        // then
        verifyOrder {
            reservationRepository.findByIdForUpdate(1L)
            paymentRepository.findByReservationIdForUpdate(1L)
        }
        verify(exactly = 0) { paymentRepository.findByPaymentReferenceForUpdate(any()) }
    }

    @DisplayName("주문번호로 잠글 때도 예약을 먼저 잠그고 결제를 나중에 잠근다")
    @Test
    fun locksReservationBeforePaymentByOrderId() {
        // given
        val fixture = pendingFixture()
        every { paymentRepository.findByOrderId("order-1") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { paymentRepository.findByReservationIdForUpdate(1L) } returns fixture.payment

        // when
        val result = service.lockReservationThenPaymentByOrderId("order-1")

        // then
        assertEquals(fixture.payment, result)
        verifyOrder {
            reservationRepository.findByIdForUpdate(1L)
            paymentRepository.findByReservationIdForUpdate(1L)
        }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }

    @DisplayName("주문번호에 맞는 결제가 없으면 아무것도 잠그지 않고 null을 돌려준다")
    @Test
    fun returnsNullWhenOrderNotFound() {
        // given
        every { paymentRepository.findByOrderId("none") } returns null

        // when
        val result = service.lockReservationThenPaymentByOrderId("none")

        // then
        assertNull(result)
        verify(exactly = 0) { reservationRepository.findByIdForUpdate(any()) }
    }

    @DisplayName("결제 실패 시 예약을 취소한다")
    @Test
    fun cancelsReservationOnPaymentFailure() {
        // given
        val fixture = pendingFixture(paymentReference = "pay_fail")
        every { paymentRepository.findByPaymentReference("pay_fail") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { paymentRepository.findByReservationIdForUpdate(1L) } returns fixture.payment

        // when
        val result = service.handlePaymentFailed("pay_fail", "카드 오류")

        // then
        assertEquals(PaymentStatus.FAILED, result.status)
        assertEquals(ReservationStatus.CANCELED, fixture.reservation.status)
        verify(exactly = 1) { reservationSlotWaitlistService.notifyWaitersForReleasedSlot(fixture.slot.id) }
    }

    @DisplayName("만료된 예약은 결제 성공 요청 시 만료 처리된다")
    @Test
    fun expiresReservationOnLatePaymentSuccess() {
        // given
        val fixture = pendingFixture(paymentReference = "pay_late", expired = true)
        every { paymentRepository.findByPaymentReference("pay_late") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { paymentRepository.findByReservationIdForUpdate(1L) } returns fixture.payment
        every { reservationCommandService.expirePendingReservation(1L, any()) } returns fixture.reservation

        // when
        val result = service.handlePaymentSucceeded("pay_late")

        // then
        assertEquals(PaymentStatus.EXPIRED, result.status)
        verify(exactly = 1) { reservationCommandService.expirePendingReservation(1L, any()) }
        verify(exactly = 0) { reservationCommandService.confirmReservation(any()) }
    }

    @DisplayName("토스 승인은 성공했는데 그 사이 예약이 만료됐으면 자동 환불한다")
    @Test
    fun refundsWhenReservationExpiredAfterApproval() {
        // given
        val fixture = pendingFixture(paymentReference = "pay_late_ok", expired = true)
        fixture.payment.markSucceeded(LocalDateTime.now())
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { reservationCommandService.expirePendingReservation(1L, any()) } returns fixture.reservation

        // when
        service.finalizeAfterGatewaySuccess(fixture.payment)

        // then
        verify(exactly = 1) { paymentRefundService.refundAfterExpiredReservation(fixture.payment, fixture.reservation) }
        verify(exactly = 0) { reservationCommandService.confirmReservation(any()) }
    }

    @DisplayName("예약이 취소된 뒤 결제가 성공하면 예외 대신 자동 환불한다")
    @Test
    fun refundsWhenReservationCanceledAfterApproval() {
        val fixture = pendingFixture(paymentReference = "pay_canceled_ok")
        fixture.reservation.cancel(ReservationCancelReason.USER_REQUESTED)
        fixture.payment.markSucceeded(LocalDateTime.now())
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation

        service.finalizeAfterGatewaySuccess(fixture.payment)

        verify(exactly = 1) { paymentRefundService.refundAfterExpiredReservation(fixture.payment, fixture.reservation) }
        verify(exactly = 0) { reservationCommandService.confirmReservation(any()) }
    }

    private fun pendingFixture(
        paymentReference: String = "pay_ok",
        expired: Boolean = false,
    ): PaymentFixture {
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
        val expiresAt =
            if (expired) {
                LocalDateTime.now().minusMinutes(1)
            } else {
                LocalDateTime.now().plusMinutes(15)
            }
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
        val payment = Payment.createMock(reservation, paymentReference)
        return PaymentFixture(slot = slot, reservation = reservation, payment = payment)
    }

    private data class PaymentFixture(
        val slot: com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot,
        val reservation: Reservation,
        val payment: Payment,
    )
}
