package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("기본 카드 바로 예약")
class QuickBookServiceTest {
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val reservationCommandService = mockk<ReservationCommandService>()
    private val confirmTxService = mockk<TossPaymentConfirmTxService>()
    private val reservationPaymentService = mockk<ReservationPaymentService>(relaxed = true)
    private val tossClient = mockk<TossPaymentsApiClient>()
    private val service =
        QuickBookService(
            methodRepository,
            reservationCommandService,
            confirmTxService,
            reservationPaymentService,
            tossClient,
        )
    private val method = TossBillingMethod(userId = USER_ID, customerKey = "idolglow-user-7", billingKey = "bk-7")
    private val payment = Payment.createPendingForToss(reservation(), "pay_toss_1")
    private val command =
        CreateReservationCommand(
            productId = 1L,
            reservationSlotId = 2L,
            userId = USER_ID,
            totalPrice = BigDecimal("9000.00"),
            bookingChannel = ReservationBookingChannel.QUICK_BOOK,
        )

    @BeforeEach
    fun setUp() {
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns method
        every { reservationCommandService.createReservation(command) } returns
            ReservationCreatedResponse.from(payment.reservation, payment)
        every { confirmTxService.prepareConfirm(USER_ID, payment.orderId, payment.amount, null) } returns
            TossConfirmPrepareResult(payment, skipGateway = false, idempotencyKey = "k")
    }

    @DisplayName("R2 기본 카드가 없으면 400 BILLING_METHOD_REQUIRED 이고 예약을 만들지 않는다")
    @Test
    fun noDefaultCardRejected() {
        // given
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns null

        // when
        val error = catchThrowableOfType(CustomException::class.java) { service.book(command) }

        // then
        assertThat(error.getExceptionType()).isEqualTo(QuickBookExceptionType.BILLING_METHOD_REQUIRED)
        assertThat(error.getExceptionType().httpStatusCode).isEqualTo(400)
        verify(exactly = 0) { reservationCommandService.createReservation(any()) }
    }

    @DisplayName("R4 기본 카드 빌링키로 예약 주문 번호와 금액을 승인하고 승인 응답을 반영한다")
    @Test
    fun chargesDefaultCardWithReservationOrder() {
        // given
        val response = TossApiResponse(200, null, "{}")
        every { tossClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } returns response
        every { confirmTxService.completeConfirm(USER_ID, payment.orderId, payment.amount, response) } answers {
            payment.status = PaymentStatus.SUCCEEDED
            payment
        }

        // when
        val result = service.book(command)

        // then
        assertThat(result.status).isEqualTo(PaymentStatus.SUCCEEDED)
        verify {
            tossClient.chargeBilling(
                billingKey = "bk-7",
                customerKey = "idolglow-user-7",
                amount = 9_000L,
                orderId = payment.orderId,
                orderName = any(),
                customerEmail = null,
                customerName = null,
                idempotencyKey = "quick-book-${payment.orderId}",
            )
        }
    }

    @DisplayName("R6 승인이 실패하면 결제 실패 처리로 예약을 취소하고 402 QUICK_BOOK_PAYMENT_FAILED 를 낸다")
    @Test
    fun failedChargeCancelsReservation() {
        // given
        val response = TossApiResponse(400, null, "{}")
        every { tossClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } returns response
        every { confirmTxService.completeConfirm(USER_ID, payment.orderId, payment.amount, response) } answers {
            payment.status = PaymentStatus.FAILED
            payment
        }

        // when
        val error = catchThrowableOfType(CustomException::class.java) { service.book(command) }

        // then
        assertThat(error.getExceptionType()).isEqualTo(QuickBookExceptionType.PAYMENT_FAILED)
        assertThat(error.getExceptionType().httpStatusCode).isEqualTo(402)
        verify { reservationPaymentService.handlePaymentFailed(payment.paymentReference, any()) }
    }

    @DisplayName("R7 응답이 불확실하면 승인 반영을 하지 않고 결제를 그대로 돌려준다")
    @Test
    fun inconclusiveLeavesConfirming() {
        // given
        payment.status = PaymentStatus.CONFIRMING
        every { tossClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            TossApiResponse(503, null, null)

        // when
        val result = service.book(command)

        // then
        assertThat(result.status).isEqualTo(PaymentStatus.CONFIRMING)
        verify(exactly = 0) { confirmTxService.completeConfirm(any(), any(), any(), any()) }
        verify(exactly = 0) { reservationPaymentService.handlePaymentFailed(any(), any()) }
    }

    private fun reservation(): Reservation {
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns 1L
        val date = LocalDate.of(2026, 12, 20)
        val slot =
            ReservationSlot(
                product = product,
                reservationDate = date,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
            )
        return Reservation(
            reservationSlot = slot,
            userId = USER_ID,
            visitDate = date,
            visitStartTime = LocalTime.of(10, 0),
            visitEndTime = LocalTime.of(11, 0),
            totalPrice = BigDecimal("9000.00"),
            bookingChannel = ReservationBookingChannel.QUICK_BOOK,
        )
    }

    private companion object {
        const val USER_ID = 7L
    }
}
