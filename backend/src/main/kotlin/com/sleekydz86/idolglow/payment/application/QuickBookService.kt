package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QuickBookService(
    private val billingMethodRepository: TossBillingMethodJpaRepository,
    private val reservationCommandService: ReservationCommandService,
    private val confirmTxService: TossPaymentConfirmTxService,
    private val reservationPaymentService: ReservationPaymentService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun book(command: CreateReservationCommand): PaymentResponse {
        val method =
            billingMethodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(command.userId)
                ?: throw CustomException(QuickBookExceptionType.BILLING_METHOD_REQUIRED)
        val created = reservationCommandService.createReservation(command)
        val prepared =
            confirmTxService.prepareConfirm(command.userId, created.payment.orderId, created.payment.amount, null)
        val payment = if (prepared.skipGateway) prepared.payment else charge(command.userId, method, prepared)
        return settle(payment)
    }

    private fun charge(
        userId: Long,
        method: TossBillingMethod,
        prepared: TossConfirmPrepareResult,
    ): Payment {
        val payment = prepared.payment
        val response =
            tossPaymentsApiClient.chargeBilling(
                billingKey = method.billingKey,
                customerKey = method.customerKey,
                amount = payment.amount.longValueExact(),
                orderId = payment.orderId,
                orderName = ORDER_NAME,
                customerEmail = null,
                customerName = null,
                idempotencyKey = "quick-book-${payment.orderId}",
            )
        if (response.isInconclusive) {
            log.warn("바로 예약 승인 결과가 불확실합니다 paymentId={} orderId={}", payment.id, payment.orderId)
            return payment
        }
        return confirmTxService.completeConfirm(userId, payment.orderId, payment.amount, response)
    }

    private fun settle(payment: Payment): PaymentResponse {
        if (payment.status != PaymentStatus.FAILED) {
            return PaymentResponse.from(payment)
        }
        log.warn("바로 예약 카드 결제 실패 paymentId={} orderId={}", payment.id, payment.orderId)
        reservationPaymentService.handlePaymentFailed(payment.paymentReference, payment.failureReason ?: "카드 결제 실패")
        throw CustomException(QuickBookExceptionType.PAYMENT_FAILED)
    }

    private companion object {
        const val ORDER_NAME = "IdolGlow 바로 예약"
    }
}
