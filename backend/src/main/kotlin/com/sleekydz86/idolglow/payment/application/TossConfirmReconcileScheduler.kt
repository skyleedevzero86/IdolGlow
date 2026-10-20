package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TossConfirmReconcileScheduler(
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val tossPaymentConfirmTxService: TossPaymentConfirmTxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.confirm-reconcile-interval-ms:15000}")
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
            !remote.isSuccess2xx || remote.json == null -> leaveOrFailIfStale(payment, remote, "토스 주문 조회 실패")
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
                tossPaymentConfirmTxService.completeConfirm(
                    payment.reservation.userId,
                    payment.orderId,
                    payment.amount,
                    remote,
                )
            "ABORTED", "EXPIRED", "CANCELED" -> failUnresolved(payment, "토스 결제가 $status 상태입니다.")
            else -> leaveOrFailIfStale(payment, remote, "토스 결제 status=$status")
        }
    }

    private fun leaveOrFailIfStale(
        payment: Payment,
        remote: TossApiResponse,
        detail: String,
    ) {
        log.warn("토스 주문 조회가 결론이 아닙니다. paymentId={} httpStatus={} detail={}", payment.id, remote.httpStatus, detail)
        if (!isStale(payment)) {
            return
        }
        failUnresolved(payment, "토스 결제가 일정 시간 결론이 나지 않았습니다. $detail")
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
        private const val BATCH_SIZE = 100
        private const val RECONCILE_AFTER_SECONDS = 5L
        private const val STALE_AFTER_MINUTES = 10L
        private const val HTTP_NOT_FOUND = 404
    }
}
