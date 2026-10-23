package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import tools.jackson.databind.ObjectMapper
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("토스 결제 승인")
class TossPaymentConfirmServiceTest {
    private val tossPaymentConfirmTxService = mockk<TossPaymentConfirmTxService>()
    private val paymentRepository = mockk<PaymentRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val paymentRefundService = mockk<PaymentRefundService>(relaxed = true)

    private lateinit var service: TossPaymentConfirmService

    @BeforeEach
    fun setUp() {
        service =
            TossPaymentConfirmService(
                tossPaymentConfirmTxService = tossPaymentConfirmTxService,
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                paymentLogCommandService = paymentLogCommandService,
                paymentRefundService = paymentRefundService,
            )
    }

    @DisplayName("예약이 만료됐으면 토스 승인을 부르지 않고 결제를 만료 처리한다")
    @Test
    fun skipsTossConfirmWhenReservationExpired() {
        // given
        val payment = tossPayment(expired = true)
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = true, idempotencyKey = "confirm-${payment.orderId}")

        // when
        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.EXPIRED, result.status)
        verify(exactly = 0) { tossPaymentsApiClient.confirm(any(), any(), any(), any()) }
    }

    @DisplayName("멱등 키가 없으면 결제마다 고정된 키로 토스 승인을 요청한다")
    @Test
    fun usesStableIdempotencyKeyPerPayment() {
        // given
        val payment = tossPayment()
        val keySlot = slot<String?>()
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "confirm-${payment.orderId}")
        every { tossPaymentsApiClient.confirm(any(), any(), any(), captureNullable(keySlot)) } returns
            TossApiResponse(400, null, """{"message":"승인 거절"}""")
        every {
            tossPaymentConfirmTxService.completeConfirm(10L, payment.orderId, BigDecimal("35000"), any())
        } returns payment

        // when
        service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals("confirm-${payment.orderId}", keySlot.captured)
    }

    @DisplayName("토스 승인 응답이 5xx면 처리 중으로 두고 승인을 끝내지 않는다")
    @Test
    fun leavesConfirmingWhenTossReturns5xx() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "confirm-${payment.orderId}")
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")
        every { paymentRepository.findByOrderId(payment.orderId) } returns payment

        // when
        val result = service.confirm(10L, "pk-5xx", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.CONFIRMING, result.status)
        verify(exactly = 0) { tossPaymentConfirmTxService.completeConfirm(any(), any(), any(), any()) }
    }

    @DisplayName("토스 승인이 409 멱등 처리 중이면 실패로 끝내지 않고 승인 처리 중으로 남긴다")
    @Test
    fun confirmIdempotentProcessingTreatedAsUnknown() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        val body = """{"code":"IDEMPOTENT_REQUEST_PROCESSING"}"""
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "confirm-${payment.orderId}")
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(409, ObjectMapper().readTree(body), body)
        every { paymentRepository.findByOrderId(payment.orderId) } returns payment

        // when
        val result = service.confirm(10L, "pk-409", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.CONFIRMING, result.status)
        verify(exactly = 0) { tossPaymentConfirmTxService.completeConfirm(any(), any(), any(), any()) }
    }

    @DisplayName("이미 환불 처리 중인 결제를 다시 확인해도 정산 실패가 응답을 깨지 않는다")
    @Test
    fun skipGatewayKeepsResponseWhenRefundSettlementFails() {
        // given
        val payment = tossPayment()
        payment.markSucceeded()
        payment.markRefunding()
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = true, idempotencyKey = "confirm-${payment.orderId}")
        every { paymentRefundService.settlePreparedRefund(payment.id) } throws IllegalStateException("정산 실패")

        // when
        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.REFUNDING, result.status)
        verify(exactly = 0) { tossPaymentsApiClient.confirm(any(), any(), any(), any()) }
        verify(exactly = 1) { paymentRefundService.settlePreparedRefund(payment.id) }
        verify(exactly = 1) {
            paymentLogCommandService.append(
                payment = payment,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                logType = PaymentLogType.CANCEL_ERROR,
                step = PaymentLogStep.SERVER,
                errorMessage = "승인 뒤 환불 정산에 실패했습니다. paymentId=${payment.id}",
            )
        }
    }

    @DisplayName("승인만 끝난 결제는 환불 정산을 조회하지 않는다")
    @Test
    fun confirmSkipsRefundSettlementUnlessRefunding() {
        // given
        val payment = tossPayment()
        payment.markSucceeded()
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "confirm-${payment.orderId}")
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(200, null, """{"status":"DONE"}""")
        every {
            tossPaymentConfirmTxService.completeConfirm(10L, payment.orderId, BigDecimal("35000"), any())
        } returns payment

        // when
        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        verify(exactly = 0) { paymentRefundService.settlePreparedRefund(any()) }
    }

    private fun tossPayment(expired: Boolean = false): Payment {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.userId } returns 10L
        every { reservation.totalPrice } returns BigDecimal("35000")
        every { reservation.status } returns ReservationStatus.PENDING
        every { reservation.isExpired(any()) } returns expired
        val payment = Payment.createPendingForToss(reservation, "pay_toss_1")
        if (expired) {
            payment.markExpired()
        }
        return payment
    }
}
