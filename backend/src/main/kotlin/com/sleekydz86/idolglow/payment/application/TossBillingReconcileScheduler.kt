package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingReconcileTarget
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TossBillingReconcileScheduler(
    private val attemptRepository: TossBillingChargeAttemptJpaRepository,
    private val billingChargeReconcileTxService: BillingChargeReconcileTxService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val notificationCommandService: NotificationCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.billing-reconcile-interval-ms:300000}")
    @SchedulerLock(
        name = "tossBillingReconcile",
        lockAtLeastFor = "PT1M",
        lockAtMostFor = "PT4M",
    )
    fun reconcilePendingBillingAttempts() {
        reconcilePendingBillingAttempts(LocalDateTime.now())
    }

    fun reconcilePendingBillingAttempts(now: LocalDateTime) {
        val targets =
            attemptRepository.findLatestPendingAttempts(
                pending = TossBillingChargeStatus.PENDING,
                createdBefore = now.minusMinutes(MIN_AGE_MINUTES),
                pageable = PageRequest.of(0, BATCH_SIZE),
            )
        targets.forEach { target ->
            runCatching { reconcileOne(target, now) }
                .onFailure { error ->
                    log.warn(
                        "정기결제 조회 실패 chargeId={} attemptId={} tossOrderId={}",
                        target.chargeId,
                        target.attemptId,
                        target.tossOrderId,
                        error,
                    )
                }
        }
    }

    private fun reconcileOne(
        target: BillingReconcileTarget,
        now: LocalDateTime,
    ) {
        val remote = tossPaymentsApiClient.getPaymentByOrderId(target.tossOrderId)
        val applied =
            billingChargeReconcileTxService.apply(
                chargeId = target.chargeId,
                attemptId = target.attemptId,
                remote = remote,
                now = now,
            )
        noteOutcome(target, remote, now, applied)
    }

    private fun noteOutcome(
        target: BillingReconcileTarget,
        remote: TossApiResponse,
        now: LocalDateTime,
        applied: BillingReconcileApplyResult,
    ) {
        if (applied.outcome == BillingReconcileOutcome.AMOUNT_MISMATCH) {
            log.error(
                "정기결제 조회 금액이 회차 금액과 다릅니다. chargeId={} orderId={} amount={}",
                target.chargeId,
                target.tossOrderId,
                applied.amount,
            )
        }
        if (applied.outcome == BillingReconcileOutcome.FAILED) {
            notifyFailed(applied)
        }
        val stillPending =
            applied.outcome == BillingReconcileOutcome.PENDING ||
                applied.outcome == BillingReconcileOutcome.AMOUNT_MISMATCH
        val stale = !target.createdAt.isAfter(now.minusHours(STALE_HOURS))
        if (stillPending && stale) {
            log.error(
                "정기결제 승인 결과가 1시간 넘게 확인되지 않음, 관리자 확인 필요. " +
                    "chargeId={} attemptId={} tossOrderId={} createdAt={} httpStatus={}",
                target.chargeId,
                target.attemptId,
                target.tossOrderId,
                target.createdAt,
                remote.httpStatus,
            )
        }
    }

    private fun notifyFailed(applied: BillingReconcileApplyResult) {
        val userId = applied.userId ?: return
        val message = applied.failureMessage ?: return
        runCatching {
            notificationCommandService.create(
                userId = userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "Glow Pass 결제 실패",
                message = "자동결제에 실패했습니다. 다른 카드로 다시 결제할 수 있습니다. ($message)",
                link = "/billing",
            )
        }.onFailure { error ->
            log.warn("빌링 실패 알림 발송 실패 userId={}", userId, error)
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val MIN_AGE_MINUTES = 1L
        private const val STALE_HOURS = 1L
    }
}
