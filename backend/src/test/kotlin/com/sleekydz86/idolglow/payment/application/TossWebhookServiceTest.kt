package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.MockKVerificationScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
    private val paymentLogJpaRepository = mockk<PaymentLogJpaRepository>(relaxed = true)
    private val reservationPaymentService = mockk<ReservationPaymentService>()
    private val reservationPaymentLock = mockk<ReservationPaymentLock>()

    private lateinit var service: TossWebhookService

    @BeforeEach
    fun setUp() {
        val txService =
            TossWebhookTxService(
                reservationPaymentLock = reservationPaymentLock,
                reservationPaymentService = reservationPaymentService,
                paymentRefundTxService = mockk(relaxed = true),
                paymentLogCommandService = paymentLogCommandService,
                paymentLogJpaRepository = paymentLogJpaRepository,
            )
        service =
            TossWebhookService(
                objectMapper = objectMapper,
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
                paymentLogCommandService = paymentLogCommandService,
                tossWebhookTxService = txService,
            )
    }

    @DisplayName("웹훅도 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun webhookUsesReservationFirstLock() {
        // given
        val payment = pendingTossPayment()
        val body = statusChangedBody(payment.orderId, "READY")
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "READY", 35000)

        // when
        service.handlePayload(body)

        // then
        verifyOrder {
            tossPaymentsApiClient.getPayment("pk-1")
            reservationPaymentLock.lockByOrderId(payment.orderId)
        }
        verify(exactly = 1) { reservationPaymentLock.lockByOrderId(payment.orderId) }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
        verify(exactly = 0) { reservationPaymentService.lockReservationThenPaymentByOrderId(any()) }
    }

    @DisplayName("본문 status가 DONE이어도 토스 조회 상태가 대기면 성공 처리하지 않는다")
    @Test
    fun webhookBodyIsNotTrusted() {
        // given
        val payment = pendingTossPayment()
        val body = statusChangedBody(payment.orderId, "DONE")
        stubLockedPayment(payment)
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
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "DONE", 1)

        // when
        service.handlePayload(body)

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
    }

    @DisplayName("실패로 정리된 결제가 토스에서 승인되면 확인 로그만 남긴다")
    @Test
    fun lateApprovalOnFailedPaymentIsFlagged() {
        // given
        val payment = failedTossPayment()
        val reservationStatus = payment.reservation.status
        val body = statusChangedBody(payment.orderId, "DONE")
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "DONE", 35000)
        every {
            paymentLogJpaRepository.existsByPaymentIdAndErrorMessage(
                payment.id,
                TossWebhookService.LATE_APPROVAL_MESSAGE,
            )
        } returns false

        // when
        service.handlePayload(body)

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.reservation.status).isEqualTo(reservationStatus)
        assertThat(payment.reservation.status).isEqualTo(ReservationStatus.PENDING)
        assertThat(payment.paymentKey).isNull()
        assertThat(payment.approvedAt).isNull()
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
        verify(exactly = 1) { lateApprovalLog(payment) }
    }

    @DisplayName("같은 실패 결제의 늦은 승인 확인 로그는 한 건만 남긴다")
    @Test
    fun lateApprovalFlagIsWrittenOnce() {
        // given
        val payment = failedTossPayment()
        val body = statusChangedBody(payment.orderId, "DONE")
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "DONE", 35000)
        every {
            paymentLogJpaRepository.existsByPaymentIdAndErrorMessage(
                payment.id,
                TossWebhookService.LATE_APPROVAL_MESSAGE,
            )
        } returnsMany listOf(false, true)

        // when
        service.handlePayload(body)
        service.handlePayload(body)

        // then
        verify(exactly = 1) { lateApprovalLog(payment) }
        verify(exactly = 2) { tossPaymentsApiClient.getPayment("pk-1") }
    }

    @DisplayName("실패로 정리된 결제의 조회가 중단이면 확인 로그를 남기지 않는다")
    @Test
    fun failedPaymentWithAbortedRemoteIsIgnored() {
        // given
        val payment = failedTossPayment()
        val body = statusChangedBody(payment.orderId, "ABORTED")
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "ABORTED", 35000)

        // when
        service.handlePayload(body)

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        verify(exactly = 1) { tossPaymentsApiClient.getPayment("pk-1") }
        verify(exactly = 0) { lateApprovalLog(payment) }
        verify(exactly = 1) { closedFailedLog(payment) }
    }

    @DisplayName("실패로 정리된 결제의 승인 금액이 다르면 확인 로그를 남기지 않는다")
    @Test
    fun failedPaymentWithAmountMismatchIsNotFlagged() {
        // given
        val payment = failedTossPayment()
        val body = statusChangedBody(payment.orderId, "DONE")
        stubLockedPayment(payment)
        every { tossPaymentsApiClient.getPayment("pk-1") } returns remotePayment(payment.orderId, "DONE", 1)

        // when
        service.handlePayload(body)

        // then
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.approvedAt).isNull()
        verify(exactly = 1) { tossPaymentsApiClient.getPayment("pk-1") }
        verify(exactly = 0) { lateApprovalLog(payment) }
        verify(exactly = 1) { closedFailedLog(payment) }
    }

    private inline fun MockKVerificationScope.lateApprovalLog(payment: Payment) {
        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            logType = PaymentLogType.CONFIRM_ERROR,
            step = PaymentLogStep.SERVER,
            httpStatus = 200,
            responseBody = any(),
            errorMessage = TossWebhookService.LATE_APPROVAL_MESSAGE,
        )
    }

    private inline fun MockKVerificationScope.closedFailedLog(payment: Payment) {
        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            logType = PaymentLogType.SYSTEM,
            step = PaymentLogStep.SERVER,
            requestBody = any(),
            errorMessage = "종료된 결제 상태: FAILED",
        )
    }

    private fun stubLockedPayment(payment: Payment) {
        every { paymentRepository.findByOrderId(payment.orderId) } returns payment
        every { reservationPaymentLock.lockByOrderId(payment.orderId) } returns payment
    }

    private fun statusChangedBody(
        orderId: String,
        status: String,
    ): String = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"$orderId","paymentKey":"pk-1","status":"$status"}}"""

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

    private fun failedTossPayment(): Payment {
        val payment = pendingTossPayment()
        payment.markFailed("토스 결제가 일정 시간 결론이 나지 않았습니다.")
        return payment
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
