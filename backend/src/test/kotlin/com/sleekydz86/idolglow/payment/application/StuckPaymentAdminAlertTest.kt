package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.PaymentAdminAlertProperties
import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.payment.infrastructure.WebhookStuckPaymentAdminNotifier
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("멈춘 결제 관리자 알림")
class StuckPaymentAdminAlertTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val objectMapper = ObjectMapper()

    @Test
    fun stuckConfirmingTriggersAdminAlertOnce() {
        val client = mockk<TossPaymentsApiClient>()
        val notifier = RecordingStuckPaymentNotifier()
        val scheduler =
            TossConfirmReconcileScheduler(
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = client,
                tossPaymentConfirmTxService = mockk(relaxed = true),
                paymentRefundService = mockk(relaxed = true),
                paymentLogCommandService = mockk(relaxed = true),
                paymentLogJpaRepository = mockk<PaymentLogJpaRepository>(relaxed = true),
                stuckPaymentAdminAlertService =
                    StuckPaymentAdminAlertService(
                        claim = MemoryStuckAlertClaim(),
                        notifier = notifier,
                    ),
            )
        val payment = confirmingPayment(77L, minutesAgo = 11)
        every { paymentRepository.findConfirmingUpdatedBefore(any(), any()) } returns listOf(payment)
        every { client.getPaymentByOrderId(payment.orderId) } returns inProgressResponse()
        val cutoff = LocalDateTime.now().plusMinutes(1)

        scheduler.reconcileConfirmingPayments(cutoff)
        scheduler.reconcileConfirmingPayments(cutoff)

        assertEquals(1, notifier.alerts.size)
        val alert = notifier.alerts.single()
        assertEquals("77", alert.paymentId)
        assertEquals(payment.orderId, alert.orderId)
        assertEquals(PaymentStatus.CONFIRMING.name, alert.status)
        assertTrue(alert.lastTossLookup.contains("IN_PROGRESS"))
        val text = alert.describe()
        assertTrue(text.contains("결제 ID: 77"))
        assertTrue(text.contains("주문 ID: ${payment.orderId}"))
        assertTrue(text.contains("상태: CONFIRMING"))
        assertTrue(text.contains("멈춘 시간:"))
        assertTrue(text.contains("마지막 토스 조회 결과:"))
        assertTrue(text.contains("IN_PROGRESS"))
    }

    @Test
    fun missingAlertTokenDoesNotSend() {
        val properties = PaymentAdminAlertProperties()
        assertEquals("", properties.discordWebhookUrl)
        assertEquals("", properties.telegramBotToken)
        assertEquals("", properties.telegramChatId)
        val sent =
            WebhookStuckPaymentAdminNotifier(properties).notify(
                StuckPaymentAlert(
                    subjectKey = "confirming:1",
                    paymentId = "1",
                    orderId = "ORD_1",
                    status = PaymentStatus.CONFIRMING.name,
                    stuckSince = LocalDateTime.now().minusMinutes(11),
                    lastTossLookup = "http=200 status=IN_PROGRESS",
                    linkedPaymentId = 1L,
                ),
            )
        assertFalse(sent)
    }

    private fun confirmingPayment(
        id: Long,
        minutesAgo: Long,
    ): Payment {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.totalPrice } returns BigDecimal("35000")
        val payment = Payment.createPendingForToss(reservation, "pay_$id")
        payment.markConfirming()
        setField(payment, "id", id)
        setField(payment, "updatedAt", LocalDateTime.now().minusMinutes(minutesAgo))
        return payment
    }

    private fun setField(
        target: Any,
        name: String,
        value: Any,
    ) {
        val owner = if (name == "updatedAt") BaseEntity::class.java else target.javaClass
        val field = owner.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun inProgressResponse(): TossApiResponse {
        val body = """{"status":"IN_PROGRESS"}"""
        val json = objectMapper.readTree(body)
        return TossApiResponse(200, json, body)
    }
}

private class RecordingStuckPaymentNotifier : StuckPaymentAdminNotifier {
    val alerts = mutableListOf<StuckPaymentAlert>()

    override fun notify(alert: StuckPaymentAlert): Boolean {
        alerts += alert
        return true
    }
}

private class MemoryStuckAlertClaim : StuckPaymentAlertClaim {
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
