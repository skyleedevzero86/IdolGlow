package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class ReservationPaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val reservationCommandService = mockk<ReservationCommandService>(relaxed = true)
    private val notificationCommandService = mockk<NotificationCommandService>(relaxed = true)
    private val reservationSlotWaitlistService = mockk<ReservationSlotWaitlistService>(relaxed = true)
    private val paymentNotificationMailService = mockk<PaymentNotificationMailService>(relaxed = true)

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
            )
    }

    @Test
    fun `결제 성공 시 예약을 확정한다`() {
        val fixture = pendingFixture()
        every { paymentRepository.findByPaymentReferenceForUpdate("pay_ok") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { reservationCommandService.confirmReservation(1L) } answers {
            fixture.reservation.confirm()
            fixture.reservation
        }

        val result = service.handlePaymentSucceeded("pay_ok")

        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        verify(exactly = 1) { reservationCommandService.confirmReservation(1L) }
        verify(exactly = 1) { paymentNotificationMailService.sendSucceeded(fixture.payment) }
    }

    @Test
    fun `결제 실패 시 예약을 취소한다`() {
        val fixture = pendingFixture(paymentReference = "pay_fail")
        every { paymentRepository.findByPaymentReferenceForUpdate("pay_fail") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation

        val result = service.handlePaymentFailed("pay_fail", "카드 오류")

        assertEquals(PaymentStatus.FAILED, result.status)
        assertEquals(ReservationStatus.CANCELED, fixture.reservation.status)
        verify(exactly = 1) { reservationSlotWaitlistService.notifyWaitersForReleasedSlot(fixture.slot.id) }
    }

    @Test
    fun `만료된 예약은 결제 성공 요청 시 만료 처리된다`() {
        val fixture = pendingFixture(paymentReference = "pay_late", expired = true)
        every { paymentRepository.findByPaymentReferenceForUpdate("pay_late") } returns fixture.payment
        every { reservationRepository.findByIdForUpdate(1L) } returns fixture.reservation
        every { reservationCommandService.expirePendingReservation(1L, any()) } returns fixture.reservation

        val result = service.handlePaymentSucceeded("pay_late")

        assertEquals(PaymentStatus.EXPIRED, result.status)
        verify(exactly = 1) { reservationCommandService.expirePendingReservation(1L, any()) }
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
