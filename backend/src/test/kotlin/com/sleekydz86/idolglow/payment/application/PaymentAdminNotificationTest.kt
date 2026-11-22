package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.AdminNotificationPublisher
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("결제 관리자 알림 요청")
class PaymentAdminNotificationTest {
    @DisplayName("예약 결제가 확정되면 금액과 주문 번호로 관리자 결제 완료 알림을 한 번 요청한다")
    @Test
    fun reservationPaymentSuccessPublishesCompleted() {
        // given
        val published = mutableListOf<AdminNotificationRequestedEvent>()
        val paymentRepository = mockk<PaymentRepository>()
        val reservationRepository = mockk<ReservationRepository>()
        val reservationCommandService = mockk<ReservationCommandService>(relaxed = true)
        val entityManager = mockk<EntityManager>()
        val reservation = pendingReservation()
        val payment = Payment.createMock(reservation, "pay_admin")
        val query = mockk<Query>()
        every { entityManager.createNativeQuery(match<String> { it.contains("payment_reference") }) } returns query
        every { query.setParameter(any<String>(), "pay_admin") } returns query
        every { query.resultList } returns listOf(arrayOf<Any>(0L, reservation.id))
        every { reservationRepository.findByIdForUpdate(reservation.id) } returns reservation
        every { paymentRepository.findByIdForUpdate(0L) } returns payment
        every { reservationCommandService.confirmReservation(reservation.id) } answers { reservation.confirm() }
        val service =
            ReservationPaymentService(
                paymentRepository = paymentRepository,
                reservationRepository = reservationRepository,
                reservationCommandService = reservationCommandService,
                notificationCommandService = mockk<NotificationCommandService>(relaxed = true),
                reservationSlotWaitlistService = mockk(relaxed = true),
                paymentNotificationMailService = mockk(relaxed = true),
                paymentRefundService = mockk(relaxed = true),
                reservationPaymentLock =
                    ReservationPaymentLock(entityManager, reservationRepository, paymentRepository),
                adminNotificationPublisher = AdminNotificationPublisher { published += it },
            )

        // when
        service.handlePaymentSucceeded("pay_admin")
        service.handlePaymentSucceeded("pay_admin")

        // then
        val event = published.single()
        assertEquals(NotificationType.ADMIN_PAYMENT_COMPLETED, event.type)
        assertEquals("35,000원 결제가 완료되었어요. (주문 ${payment.orderId})", event.message)
        assertEquals("/admin/payments", event.link)
    }

    @DisplayName("멈춘 결제 알림은 웹훅이 꺼져 있어도 날짜별 중복 제거 링크로 앱 알림을 요청한다")
    @Test
    fun stuckPaymentPublishesDeduplicatedIssue() {
        // given
        val published = mutableListOf<AdminNotificationRequestedEvent>()
        val alertService =
            StuckPaymentAdminAlertService(
                claim = MemoryClaim(),
                notifier = { false },
                adminNotificationPublisher = AdminNotificationPublisher { published += it },
            )
        val alert =
            StuckPaymentAlert(
                subjectKey = "confirming:77",
                paymentId = "77",
                orderId = "ORD_77",
                status = "CONFIRMING",
                stuckSince = LocalDateTime.now().minusMinutes(15),
                lastTossLookup = "http=200 status=IN_PROGRESS",
            )

        // when
        alertService.notifyOncePerDay(alert)

        // then
        val event = published.single()
        assertEquals(NotificationType.ADMIN_PAYMENT_ISSUE, event.type)
        assertTrue(event.deduplicateByLink)
        assertEquals("/admin/payments#issue-confirming-77-${LocalDate.now()}", event.link)
        assertTrue(event.message.startsWith("결제 확인이 필요한 건이 있어요: 결제 ID: 77"))
        assertFalse(event.message.contains("billingKey"))
    }

    private fun pendingReservation(): Reservation {
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
        return Reservation(
            id = 1L,
            reservationSlot = slot,
            userId = 10L,
            visitDate = slot.reservationDate,
            visitStartTime = slot.startTime,
            visitEndTime = slot.endTime,
            totalPrice = BigDecimal("35000"),
        ).request(expiresAt)
    }

    private class MemoryClaim : StuckPaymentAlertClaim {
        private val sent = mutableSetOf<String>()

        override fun alreadySent(
            subjectKey: String,
            day: LocalDate,
        ): Boolean = "$subjectKey:$day" in sent

        override fun markSent(
            subjectKey: String,
            day: LocalDate,
            alert: StuckPaymentAlert,
        ) {
            sent += "$subjectKey:$day"
        }
    }
}
