package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
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
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("토스 결제 승인 트랜잭션")
class TossPaymentConfirmTxServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val reservationPaymentService = mockk<ReservationPaymentService>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val objectMapper = ObjectMapper()
    private lateinit var service: TossPaymentConfirmTxService

    @BeforeEach
    fun setUp() {
        service =
            TossPaymentConfirmTxService(
                paymentRepository = paymentRepository,
                reservationPaymentService = reservationPaymentService,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
                paymentLogCommandService = paymentLogCommandService,
            )
    }

    @DisplayName("승인 준비는 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun prepareUsesReservationFirstLock() {
        // given
        val payment = tossPayment()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result = service.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)

        // then
        assertEquals(PaymentStatus.CONFIRMING, result.payment.status)
        verify(exactly = 1) { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }

    @DisplayName("환불 처리 중인 결제는 토스 승인 없이 건너뛴다")
    @Test
    fun prepareSkipsGatewayWhenRefunding() {
        // given
        val payment = tossPayment()
        payment.markSucceeded()
        payment.markRefunding()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result = service.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)

        // then
        assertTrue(result.skipGateway)
        assertEquals(PaymentStatus.REFUNDING, result.payment.status)
    }

    @DisplayName("멱등 키 확인은 잠그지 않고 조회한다")
    @Test
    fun idempotencyCheckDoesNotLock() {
        // given
        val payment = tossPayment()
        every { paymentRepository.findByIdempotencyKey("idem-1") } returns null
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        service.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), "idem-1")

        // then
        verify(exactly = 1) { paymentRepository.findByIdempotencyKey("idem-1") }
        verify(exactly = 0) { paymentRepository.findByIdempotencyKeyForUpdate(any()) }
    }

    @DisplayName("토스 승인 실패면 실패 상태로 남긴다")
    @Test
    fun completeMarksFailedWhenTossRejects() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                TossApiResponse(400, null, """{"message":"승인 거절"}"""),
            )

        // then
        assertEquals(PaymentStatus.FAILED, result.status)
    }

    @DisplayName("토스 승인이 성공하면 예약 만료와 관계없이 승인 확정으로 보낸다")
    @Test
    fun completeAppliesSuccessWithoutExpireCheckWhenTossApproves() {
        // given
        val payment = tossPayment(expired = true)
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every { reservationPaymentService.finalizeAfterGatewaySuccess(payment) } returns payment

        // when
        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                tossDoneResponse(),
            )

        // then
        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        verify(exactly = 1) { reservationPaymentService.finalizeAfterGatewaySuccess(payment) }
    }

    @DisplayName("토스가 거절했고 예약이 닫혀 있으면 결제를 만료한다")
    @Test
    fun completeMarksExpiredWhenTossRejectsClosedReservation() {
        // given
        val payment = tossPayment(expired = true)
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                TossApiResponse(400, null, """{"message":"승인 거절"}"""),
            )

        // then
        assertEquals(PaymentStatus.EXPIRED, result.status)
    }

    @DisplayName("조회로 결론이 안 난 처리 중 결제는 실패로 넘긴다")
    @Test
    fun failUnresolvedConfirmingDelegatesToPaymentFailed() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every {
            reservationPaymentService.handlePaymentFailed(payment.paymentReference, "토스에 주문이 없습니다.")
        } returns payment

        // when
        service.failUnresolvedConfirming(10L, payment.orderId, BigDecimal("35000"), "토스에 주문이 없습니다.")

        // then
        verify(exactly = 1) {
            reservationPaymentService.handlePaymentFailed(payment.paymentReference, "토스에 주문이 없습니다.")
        }
    }

    @DisplayName("승인 응답이 입금 대기면 성공 처리하지 않고 승인 처리 중으로 남긴다")
    @Test
    fun confirmWaitingForDepositNotSucceeded() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                tossResponse("WAITING_FOR_DEPOSIT", 35000),
            )

        // then
        assertEquals(PaymentStatus.CONFIRMING, result.status)
        assertEquals(ReservationStatus.PENDING, payment.reservation.status)
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
    }

    @DisplayName("승인 응답이 완료여도 금액이 다르면 금액 불일치로 실패한다")
    @Test
    fun confirmAmountMismatchNotSucceeded() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                tossResponse("DONE", 1),
            )

        // then
        assertEquals(PaymentStatus.FAILED, result.status)
        assertTrue(result.failureReason!!.contains("금액 불일치"))
        assertTrue(result.failureReason!!.contains("35000"))
        assertTrue(result.failureReason!!.contains("1"))
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
        verify {
            paymentLogCommandService.append(
                payment = payment,
                orderId = payment.orderId,
                paymentKey = "pk-ok",
                logType = PaymentLogType.SYSTEM,
                step = PaymentLogStep.SERVER,
                httpStatus = 200,
                responseBody = any(),
                errorMessage = match { it!!.contains("금액 불일치") },
            )
        }
    }

    @DisplayName("조회에서 완료여도 금액이 다르면 실패하고 다음 조회에서 다시 처리하지 않는다")
    @Test
    fun reconcileDoneWithAmountMismatchNotSucceeded() {
        // given
        val payment = tossPayment()
        payment.markConfirming()
        val client = mockk<TossPaymentsApiClient>()
        every { paymentRepository.findConfirmingUpdatedBefore(any(), any()) } answers {
            if (payment.status == PaymentStatus.CONFIRMING) listOf(payment) else emptyList()
        }
        every { client.getPaymentByOrderId(payment.orderId) } returns tossResponse("DONE", 1)
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        val scheduler =
            TossConfirmReconcileScheduler(
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = client,
                tossPaymentConfirmTxService = service,
                paymentRefundService = mockk(relaxed = true),
            )
        val cutoff = LocalDateTime.now().plusMinutes(1)

        // when
        scheduler.reconcileConfirmingPayments(cutoff)
        scheduler.reconcileConfirmingPayments(cutoff)

        // then
        assertEquals(PaymentStatus.FAILED, payment.status)
        assertTrue(payment.failureReason!!.contains("금액 불일치"))
        verify(exactly = 0) { reservationPaymentService.finalizeAfterGatewaySuccess(any()) }
        verify(exactly = 1) { client.getPaymentByOrderId(payment.orderId) }
    }

    private fun tossDoneResponse(): TossApiResponse = tossResponse("DONE", 35000)

    private fun tossResponse(
        status: String,
        totalAmount: Long,
    ): TossApiResponse {
        val body =
            """{"paymentKey":"pk-ok","status":"$status","totalAmount":$totalAmount,""" +
                """"approvedAt":"2026-10-20T21:30:00+09:00"}"""
        val json = objectMapper.readTree(body)
        return TossApiResponse(200, json, json.toString())
    }

    private fun tossPayment(expired: Boolean = false): Payment {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.userId } returns 10L
        every { reservation.totalPrice } returns BigDecimal("35000")
        every { reservation.status } returns ReservationStatus.PENDING
        every { reservation.isExpired(any()) } returns expired
        return Payment.createPendingForToss(reservation, "pay_toss_1")
    }
}
