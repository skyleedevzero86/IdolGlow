package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
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

    private lateinit var service: TossPaymentConfirmService

    @BeforeEach
    fun setUp() {
        service =
            TossPaymentConfirmService(
                tossPaymentConfirmTxService = tossPaymentConfirmTxService,
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                paymentLogCommandService = paymentLogCommandService,
            )
    }

    @DisplayName("예약이 만료됐으면 토스 승인을 부르지 않고 결제를 만료 처리한다")
    @Test
    fun skipsTossConfirmWhenReservationExpired() {
        val payment = tossPayment(expired = true)
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = true, idempotencyKey = "confirm-${payment.orderId}")

        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        assertEquals(PaymentStatus.EXPIRED, result.status)
        verify(exactly = 0) { tossPaymentsApiClient.confirm(any(), any(), any(), any()) }
    }

    @DisplayName("멱등 키가 없으면 결제마다 고정된 키로 토스 승인을 요청한다")
    @Test
    fun usesStableIdempotencyKeyPerPayment() {
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

        service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        assertEquals("confirm-${payment.orderId}", keySlot.captured)
    }

    @DisplayName("토스 승인 응답이 5xx면 처리 중으로 두고 승인을 끝내지 않는다")
    @Test
    fun leavesConfirmingWhenTossReturns5xx() {
        val payment = tossPayment()
        payment.markConfirming()
        every {
            tossPaymentConfirmTxService.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)
        } returns TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "confirm-${payment.orderId}")
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")
        every { paymentRepository.findByOrderId(payment.orderId) } returns payment

        val result = service.confirm(10L, "pk-5xx", payment.orderId, BigDecimal("35000"), null, null)

        assertEquals(PaymentStatus.CONFIRMING, result.status)
        verify(exactly = 0) { tossPaymentConfirmTxService.completeConfirm(any(), any(), any(), any()) }
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
