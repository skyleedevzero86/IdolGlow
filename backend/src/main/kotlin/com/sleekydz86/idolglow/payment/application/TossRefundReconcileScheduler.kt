package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

@Component
class TossRefundReconcileScheduler(
    private val paymentRefundTxService: PaymentRefundTxService,
    private val paymentRefundService: PaymentRefundService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.refund-reconcile-interval-ms:300000}")
    @SchedulerLock(
        name = "tossRefundReconcile",
        lockAtLeastFor = "PT1M",
        lockAtMostFor = "PT4M",
    )
    fun reconcilePendingRefunds() {
        reconcilePendingRefunds(LocalDateTime.now())
    }

    fun reconcilePendingRefunds(now: LocalDateTime) {
        paymentRefundTxService
            .findPaymentIdsToReconcile(
                requestedBefore = now.minusMinutes(MIN_AGE_MINUTES),
                stopRetryBefore = now.minusDays(RETRY_WINDOW_DAYS),
                stopMessage = RETRY_STOPPED_MESSAGE,
                stopLoggedSince = now.minusDays(STOP_LOG_INTERVAL_DAYS),
                limit = BATCH_SIZE,
            ).forEach { paymentId ->
                runCatching { reconcileOne(paymentId, now) }
                    .onFailure { error -> log.warn("환불 조회 실패 paymentId={}", paymentId, error) }
            }
    }

    private fun reconcileOne(
        paymentId: Long,
        now: LocalDateTime,
    ) {
        val prepared = paymentRefundTxService.findPendingTossRefund(paymentId)
        val paymentKey = prepared?.paymentKey
        val refundId = prepared?.refundId
        if (prepared == null || paymentKey == null || refundId == null) {
            return
        }
        val remote = tossPaymentsApiClient.getPayment(paymentKey)
        val json = remote.json
        if (remote.isInconclusive || !remote.isSuccess2xx || json == null) {
            return
        }
        applyRemote(prepared, refundId, remote, json, now)
    }

    private fun applyRemote(
        prepared: RefundPrepareResult,
        refundId: Long,
        remote: TossApiResponse,
        json: JsonNode,
        now: LocalDateTime,
    ) {
        val payment = paymentRefundTxService.loadPayment(prepared.paymentId)
        if (TossPaymentResponseMapper.isFullyCanceled(json, payment.amount)) {
            paymentRefundTxService.completeRefund(refundId, remote)
            return
        }
        if (retryStopped(prepared.firstRequestedAt, now)) {
            logAttention(payment, remote, RETRY_STOPPED_MESSAGE)
            return
        }
        when (json.path("status").asText(null)?.trim()) {
            "DONE" -> paymentRefundService.settlePreparedRefund(payment.id)
            "PARTIAL_CANCELED" -> logAttention(payment, remote, UNOWNED_PARTIAL_CANCEL_MESSAGE)
        }
    }

    private fun retryStopped(
        firstRequestedAt: LocalDateTime?,
        now: LocalDateTime,
    ): Boolean = firstRequestedAt != null && !firstRequestedAt.isAfter(now.minusDays(RETRY_WINDOW_DAYS))

    private fun logAttention(
        payment: Payment,
        remote: TossApiResponse,
        message: String,
    ) {
        log.error("{} paymentId={}", message, payment.id)
        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            logType = PaymentLogType.CANCEL_ERROR,
            step = PaymentLogStep.SERVER,
            httpStatus = remote.httpStatus,
            responseBody = remote.rawBody,
            errorMessage = message,
        )
    }

    companion object {
        const val RETRY_STOPPED_MESSAGE = "멱등 키 만료 전 자동 재요청 중지, 관리자 확인 필요"
        const val UNOWNED_PARTIAL_CANCEL_MESSAGE = "우리가 하지 않은 부분 취소"
        private const val BATCH_SIZE = 100
        private const val MIN_AGE_MINUTES = 1L
        private const val RETRY_WINDOW_DAYS = 14L
        private const val STOP_LOG_INTERVAL_DAYS = 1L
    }
}
