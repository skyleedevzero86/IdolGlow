package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BillingChargeReconcileTxService(
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val attemptRepository: TossBillingChargeAttemptJpaRepository,
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun apply(
        chargeId: Long,
        attemptId: Long,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingReconcileApplyResult {
        val charge =
            chargeRepository.findByIdForUpdate(chargeId)
                ?: throw IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
        val attempt =
            attemptRepository.findById(attemptId).orElseThrow {
                IllegalArgumentException("결제 시도를 찾을 수 없습니다. attemptId=$attemptId")
            }
        val open =
            attempt.chargeId == charge.id &&
                charge.status == TossBillingChargeStatus.PENDING &&
                attempt.status == TossBillingChargeStatus.PENDING
        return if (open) {
            reflect(charge, attempt, remote, now)
        } else {
            BillingReconcileApplyResult(BillingReconcileOutcome.SKIPPED)
        }
    }

    private fun reflect(
        charge: TossBillingCharge,
        attempt: TossBillingChargeAttempt,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingReconcileApplyResult {
        val kind = kindOf(remote, charge.amount)
        return when (kind) {
            ReconcileKind.MATCHED -> markDone(charge, attempt, remote, now)
            ReconcileKind.MISMATCH ->
                BillingReconcileApplyResult(BillingReconcileOutcome.AMOUNT_MISMATCH, amount = charge.amount)
            ReconcileKind.TERMINAL -> markFailed(charge, attempt, remote, now)
            ReconcileKind.OPEN -> BillingReconcileApplyResult(BillingReconcileOutcome.PENDING)
        }
    }

    private fun markDone(
        charge: TossBillingCharge,
        attempt: TossBillingChargeAttempt,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingReconcileApplyResult {
        val json = remote.json
        val paymentKey = json?.path("paymentKey")?.asText(null)
        val approvedAt = IdolGlowDateTimes.parseDateTime(json?.path("approvedAt")?.asText(null)) ?: now
        attempt.markDone(paymentKey, remote.rawBody, approvedAt)
        charge.markDone(paymentKey, remote.rawBody, approvedAt)
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.markCharged(now)
        return BillingReconcileApplyResult(BillingReconcileOutcome.DONE)
    }

    private fun markFailed(
        charge: TossBillingCharge,
        attempt: TossBillingChargeAttempt,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingReconcileApplyResult {
        val message = failureMessage(remote)
        attempt.markFailed(message, remote.rawBody)
        charge.markFailed(message, remote.rawBody)
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.markChargeFailed(now)
        return BillingReconcileApplyResult(
            outcome = BillingReconcileOutcome.FAILED,
            userId = charge.userId,
            failureMessage = message,
        )
    }

    private fun failureMessage(remote: TossApiResponse): String {
        if (remote.httpStatus == HTTP_NOT_FOUND) {
            return "토스에 주문이 없습니다."
        }
        val status = remote.json?.path("status")?.asText(null)?.trim()
        return remote.json?.path("message")?.asText(null) ?: "토스 결제가 $status 상태입니다."
    }

    private fun kindOf(
        remote: TossApiResponse,
        amount: Long,
    ): ReconcileKind {
        val json = remote.json
        val status = json?.path("status")?.asText(null)?.trim()
        val expected = BigDecimal.valueOf(amount)
        val matched =
            remote.isSuccess2xx &&
                json != null &&
                TossPaymentResponseMapper.isDoneWithMatchingAmount(json, expected)
        val doneWithOtherAmount = remote.isSuccess2xx && status == "DONE" && !matched
        val terminal =
            remote.httpStatus == HTTP_NOT_FOUND ||
                (remote.isSuccess2xx && status in TERMINAL_STATUSES)
        return when {
            matched -> ReconcileKind.MATCHED
            doneWithOtherAmount -> ReconcileKind.MISMATCH
            terminal -> ReconcileKind.TERMINAL
            else -> ReconcileKind.OPEN
        }
    }

    private enum class ReconcileKind {
        MATCHED,
        MISMATCH,
        TERMINAL,
        OPEN,
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        val TERMINAL_STATUSES = setOf("ABORTED", "EXPIRED")
    }
}
