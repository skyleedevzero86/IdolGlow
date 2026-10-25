package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TossConfirmReconcileScheduler(
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val tossPaymentConfirmTxService: TossPaymentConfirmTxService,
    private val paymentRefundService: PaymentRefundService,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
    private val stuckPaymentAdminAlertService: StuckPaymentAdminAlertService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.confirm-reconcile-interval-ms:15000}")
    @SchedulerLock(
        name = "tossConfirmReconcile",
        lockAtLeastFor = "PT5S",
        lockAtMostFor = "PT1M",
    )
    fun reconcileConfirmingPayments() {
        reconcileConfirmingPayments(LocalDateTime.now().minusSeconds(RECONCILE_AFTER_SECONDS))
    }

    fun reconcileConfirmingPayments(cutoff: LocalDateTime) {
        paymentRepository.findConfirmingUpdatedBefore(cutoff, BATCH_SIZE).forEach { payment ->
            runCatching { reconcileOne(payment) }
                .onFailure { error -> log.warn("승인 처리 중 결제 조회 실패 paymentId={}", payment.id, error) }
        }
    }

    private fun reconcileOne(payment: Payment) {
        val remote = tossPaymentsApiClient.getPaymentByOrderId(payment.orderId)
        when {
            remote.httpStatus == HTTP_NOT_FOUND -> failUnresolved(payment, "토스에 주문이 없습니다.")
            !remote.isSuccess2xx || remote.json == null -> leaveAndFlagIfStale(payment, remote, "토스 주문 조회 실패")
            else -> applyRemoteStatus(payment, remote)
        }
    }

    private fun applyRemoteStatus(
        payment: Payment,
        remote: TossApiResponse,
    ) {
        val status = remote.json!!.path("status").asText(null)
        when (status) {
            "DONE" ->
                tossPaymentConfirmTxService
                    .completeConfirm(
                        payment.reservation.userId,
                        payment.orderId,
                        payment.amount,
                        remote,
                    ).let { paymentRefundService.settlePreparedRefund(it.id) }
            "ABORTED", "EXPIRED", "CANCELED" -> failUnresolved(payment, "토스 결제가 $status 상태입니다.")
            else -> leaveAndFlagIfStale(payment, remote, "토스 결제 status=$status")
        }
    }

    private fun leaveAndFlagIfStale(
        payment: Payment,
        remote: TossApiResponse,
        detail: String,
    ) {
        log.warn(
            "토스 주문 조회가 결론이 아닙니다. paymentId={} httpStatus={} detail={}",
            payment.id,
            remote.httpStatus,
            detail,
        )
        if (isStale(payment)) {
            flagNeedsReview(payment, remote)
            notifyStuckConfirming(payment, remote)
        }
    }

    private fun notifyStuckConfirming(
        payment: Payment,
        remote: TossApiResponse,
    ) {
        val since = payment.updatedAt ?: return
        stuckPaymentAdminAlertService.notifyOncePerDay(
            StuckPaymentAlert(
                subjectKey = "confirming:${payment.id}",
                paymentId = payment.id.toString(),
                orderId = payment.orderId,
                status = payment.status.name,
                stuckSince = since,
                lastTossLookup = tossLookupSummary(remote),
                linkedPaymentId = payment.id.takeIf { it > 0L },
            ),
        )
    }

    private fun flagNeedsReview(
        payment: Payment,
        remote: TossApiResponse,
    ) {
        if (paymentLogJpaRepository.existsByPaymentIdAndErrorMessage(payment.id, REVIEW_REQUIRED_MESSAGE)) {
            return
        }
        log.error("{} paymentId={}", REVIEW_REQUIRED_MESSAGE, payment.id)
        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            logType = PaymentLogType.CONFIRM_ERROR,
            step = PaymentLogStep.SERVER,
            httpStatus = remote.httpStatus,
            responseBody = remote.rawBody,
            errorMessage = REVIEW_REQUIRED_MESSAGE,
        )
    }

    private fun failUnresolved(
        payment: Payment,
        reason: String,
    ) {
        log.warn("승인 처리 중 결제를 실패로 정리합니다. paymentId={} orderId={} reason={}", payment.id, payment.orderId, reason)
        tossPaymentConfirmTxService.failUnresolvedConfirming(
            payment.reservation.userId,
            payment.orderId,
            payment.amount,
            reason,
        )
    }

    private fun isStale(payment: Payment): Boolean {
        val updatedAt = payment.updatedAt ?: return false
        return !updatedAt.isAfter(LocalDateTime.now().minusMinutes(STALE_AFTER_MINUTES))
    }

    companion object {
        const val REVIEW_REQUIRED_MESSAGE = "승인 결과가 10분 넘게 확인되지 않음, 관리자 확인 필요"
        private const val BATCH_SIZE = 100
        private const val RECONCILE_AFTER_SECONDS = 5L
        private const val STALE_AFTER_MINUTES = 10L
        private const val HTTP_NOT_FOUND = 404
    }
}
