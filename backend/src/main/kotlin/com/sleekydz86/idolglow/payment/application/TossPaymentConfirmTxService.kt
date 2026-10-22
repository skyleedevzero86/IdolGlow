package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Transactional
@Service
class TossPaymentConfirmTxService(
    private val paymentRepository: PaymentRepository,
    private val reservationPaymentService: ReservationPaymentService,
    private val tossPaymentProperties: TossPaymentProperties,
    private val paymentLogCommandService: PaymentLogCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun prepareConfirm(
        userId: Long,
        orderId: String,
        amount: BigDecimal,
        idempotencyKey: String?,
    ): TossConfirmPrepareResult {
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
        val succeeded = findSucceededByIdempotency(userId, idempotencyKey)
        return succeeded?.let { skipGateway(it, idempotencyKey.orEmpty()) }
            ?: prepareLockedPayment(userId, orderId, amount, idempotencyKey)
    }

    fun completeConfirm(
        userId: Long,
        orderId: String,
        amount: BigDecimal,
        response: TossApiResponse,
    ): Payment {
        val payment = lockOwnedTossPayment(userId, orderId, amount)
        if (payment.status == PaymentStatus.SUCCEEDED) {
            return payment
        }
        return completeOpenConfirm(payment, response)
    }

    fun failUnresolvedConfirming(
        userId: Long,
        orderId: String,
        amount: BigDecimal,
        reason: String,
    ): Payment {
        val payment = lockOwnedTossPayment(userId, orderId, amount)
        if (payment.status != PaymentStatus.CONFIRMING && payment.status != PaymentStatus.PENDING) {
            return payment
        }
        return reservationPaymentService.handlePaymentFailed(payment.paymentReference, reason)
    }

    private fun prepareLockedPayment(
        userId: Long,
        orderId: String,
        amount: BigDecimal,
        idempotencyKey: String?,
    ): TossConfirmPrepareResult {
        val payment = lockOwnedTossPayment(userId, orderId, amount)
        val open =
            payment.status == PaymentStatus.PENDING ||
                payment.status == PaymentStatus.CONFIRMING ||
                payment.status == PaymentStatus.SUCCEEDED
        require(open) { "처리할 수 없는 결제 상태입니다: ${payment.status}" }
        val skipTarget =
            if (payment.status == PaymentStatus.SUCCEEDED) {
                payment
            } else {
                markExpiredIfReservationClosed(payment)
            }
        if (skipTarget != null) {
            return skipGateway(skipTarget, skipTarget.idempotencyKey.orEmpty())
        }
        if (!idempotencyKey.isNullOrBlank()) {
            payment.idempotencyKey = idempotencyKey
        }
        if (payment.status == PaymentStatus.PENDING) {
            payment.markConfirming()
        }
        return TossConfirmPrepareResult(
            payment,
            skipGateway = false,
            idempotencyKey = idempotencyKey ?: "confirm-${payment.orderId}",
        )
    }

    private fun skipGateway(
        payment: Payment,
        idempotencyKey: String,
    ) = TossConfirmPrepareResult(payment, skipGateway = true, idempotencyKey = idempotencyKey)

    private fun findSucceededByIdempotency(
        userId: Long,
        idempotencyKey: String?,
    ): Payment? {
        val succeeded =
            idempotencyKey
                ?.let { paymentRepository.findByIdempotencyKey(it) }
                ?.takeIf { it.status == PaymentStatus.SUCCEEDED }
        if (succeeded != null) {
            require(succeeded.reservation.userId == userId) { "본인 예약 결제만 승인할 수 있습니다." }
        }
        return succeeded
    }

    private fun lockOwnedTossPayment(
        userId: Long,
        orderId: String,
        amount: BigDecimal,
    ): Payment {
        val payment =
            reservationPaymentService.lockReservationThenPaymentByOrderId(orderId)
                ?: throw IllegalArgumentException("주문을 찾을 수 없습니다: $orderId")
        require(payment.reservation.userId == userId) { "본인 예약 결제만 승인할 수 있습니다." }
        require(payment.provider == PaymentProvider.TOSS) { "토스 결제 건이 아닙니다." }
        require(payment.amount.compareTo(amount) == 0) { "결제 금액이 일치하지 않습니다." }
        return payment
    }

    private fun completeOpenConfirm(
        payment: Payment,
        response: TossApiResponse,
    ): Payment {
        if (response.isSuccess2xx && response.json != null) {
            return applyTossConfirmSuccess(payment, response)
        }
        return markExpiredIfReservationClosed(payment) ?: failTossConfirm(payment, response)
    }

    private fun applyTossConfirmSuccess(
        payment: Payment,
        response: TossApiResponse,
    ): Payment {
        val json = response.json!!
        val matched = TossPaymentResponseMapper.isDoneWithMatchingAmount(json, payment.amount)
        if (matched) {
            TossPaymentResponseMapper.applyConfirmSuccess(payment, json, response.rawBody!!)
            payment.markSucceeded(payment.approvedAt ?: LocalDateTime.now())
        } else if (json.path("status").asText(null)?.trim() == "DONE") {
            val reason = TossPaymentResponseMapper.amountMismatchReason(json, payment.amount)
            log.error(
                "토스 승인 금액이 일치하지 않습니다. paymentId={} orderId={} reason={}",
                payment.id,
                payment.orderId,
                reason,
            )
            paymentLogCommandService.append(
                payment = payment,
                orderId = payment.orderId,
                paymentKey = json.path("paymentKey").asText(null),
                logType = PaymentLogType.SYSTEM,
                step = PaymentLogStep.SERVER,
                httpStatus = response.httpStatus,
                responseBody = response.rawBody,
                errorMessage = reason,
            )
            payment.markFailed(reason)
            payment.rawResponseJson = response.rawBody
        } else if (payment.status == PaymentStatus.PENDING) {
            payment.markConfirming()
        }
        return if (matched) reservationPaymentService.finalizeAfterGatewaySuccess(payment) else payment
    }

    private fun failTossConfirm(
        payment: Payment,
        response: TossApiResponse,
    ): Payment {
        payment.markFailed(
            reason = response.json?.path("message")?.asText() ?: response.rawBody ?: "토스 승인 실패",
            code = response.json?.path("code")?.asText(),
        )
        payment.rawResponseJson = response.rawBody
        return payment
    }

    private fun markExpiredIfReservationClosed(payment: Payment): Payment? {
        val now = LocalDateTime.now()
        val reservation = payment.reservation
        if (!reservation.isExpired(now) && reservation.status != ReservationStatus.CANCELED) {
            return null
        }
        payment.markExpired(now)
        return payment
    }
}
