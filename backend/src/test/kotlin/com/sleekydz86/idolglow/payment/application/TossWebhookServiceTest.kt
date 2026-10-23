package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("토스 웹훅")
class TossWebhookServiceTest {
    private val objectMapper = ObjectMapper()
    private val paymentRepository = mockk<PaymentRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val reservationPaymentService = mockk<ReservationPaymentService>()

    private lateinit var service: TossWebhookService

    @BeforeEach
    fun setUp() {
        service =
            TossWebhookService(
                objectMapper = objectMapper,
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
                paymentLogCommandService = paymentLogCommandService,
                reservationPaymentService = reservationPaymentService,
                paymentRefundTxService = mockk(relaxed = true),
            )
    }

    @DisplayName("웹훅도 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun webhookUsesReservationFirstLock() {
        // given
        val body = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"ORD-1","paymentKey":"pk-1"}}"""
        every { reservationPaymentService.lockReservationThenPaymentByOrderId("ORD-1") } returns null

        // when
        service.handlePayload(body)

        // then
        verify(exactly = 1) { reservationPaymentService.lockReservationThenPaymentByOrderId("ORD-1") }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }

    @DisplayName("본문 status가 DONE이어도 토스 조회 상태가 대기면 성공 처리하지 않는다")
    @Test
    fun webhookBodyIsNotTrusted() {
        // given
        val payment = pendingTossPayment()
        val body = statusChangedBody(payment.orderId, "DONE")
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        val remote = remotePayment(payment.orderId, "WAITING_FOR_DEPOSIT", 35000)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remote

        // when
        service.handlePayload(body, "tx-1")

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        verify(exactly = 1) { tossPaymentsApiClient.getPayment("pk-1") }
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
        verify {
            paymentLogCommandService.append(
                payment = null,
                orderId = payment.orderId,
                paymentKey = "pk-1",
                logType = PaymentLogType.WEBHOOK_RECEIVED,
                step = PaymentLogStep.SERVER,
                httpStatus = 200,
                requestBody = body,
                traceId = "tx-1",
            )
        }
    }

    @DisplayName("토스 조회 금액이 결제 금액과 다르면 성공 처리하지 않는다")
    @Test
    fun webhookAmountMismatchIgnored() {
        // given
        val payment = pendingTossPayment()
        val body = statusChangedBody(payment.orderId, "DONE")
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "DONE", 1)

        // when
        service.handlePayload(body)

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
    }

    private fun statusChangedBody(
        orderId: String,
        status: String,
    ): String =
        """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"$orderId","paymentKey":"pk-1","status":"$status"}}"""

    private fun remotePayment(
        orderId: String,
        status: String,
        totalAmount: Long,
    ): TossApiResponse {
        val json =
            objectMapper.readTree(
                """{"orderId":"$orderId","paymentKey":"pk-1","status":"$status","totalAmount":$totalAmount}""",
            )
        return TossApiResponse(httpStatus = 200, json = json, rawBody = json.toString())
    }

    private fun pendingTossPayment(): Payment {
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
        return Payment.createPendingForToss(reservation, "pay_toss")
    }
}
