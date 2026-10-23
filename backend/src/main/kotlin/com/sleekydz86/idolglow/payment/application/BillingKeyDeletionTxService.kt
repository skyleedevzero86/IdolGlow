package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingKeyDeletionStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BillingKeyDeletionTxService(
    private val methodRepository: TossBillingMethodJpaRepository,
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepare(
        userId: Long,
        methodId: Long,
        now: LocalDateTime,
    ): BillingKeyDeletionPrepared {
        val method =
            methodRepository.findByIdAndUserIdForUpdate(methodId, userId)
                ?: throw IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
        return when (method.deletionStatus) {
            TossBillingKeyDeletionStatus.DELETED,
            TossBillingKeyDeletionStatus.DELETE_FAILED,
            -> prepared(method, callToss = false, notify = false)
            TossBillingKeyDeletionStatus.DELETE_PENDING -> prepared(method, callToss = true, notify = false)
            TossBillingKeyDeletionStatus.ACTIVE -> {
                val wasDefault = method.isDefault
                method.beginDeletion(now)
                prepared(method, callToss = true, notify = detach(method, wasDefault))
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun loadRetryKey(methodId: Long): String? {
        val method = methodRepository.findById(methodId).orElse(null)
        return method
            ?.takeIf { it.deletionStatus == TossBillingKeyDeletionStatus.DELETE_PENDING }
            ?.billingKey
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyRemote(
        methodId: Long,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingKeyDeletionApplied {
        val method =
            methodRepository.findByIdForUpdate(methodId)
                ?: throw IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
        if (method.deletionStatus != TossBillingKeyDeletionStatus.DELETE_PENDING) {
            return BillingKeyDeletionApplied(BillingKeyDeletionOutcome.SKIPPED, method.userId)
        }
        method.recordDeletionAttempt(now)
        return reflect(method, remote, now)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun acceptWebhook(
        billingKey: String,
        now: LocalDateTime,
    ): BillingKeyDeletionApplied {
        val method = methodRepository.findByBillingKeyHashForUpdate(BillingKeyCrypto.sha256Hex(billingKey))
        if (method == null || method.deletionStatus == TossBillingKeyDeletionStatus.DELETED) {
            return BillingKeyDeletionApplied(BillingKeyDeletionOutcome.SKIPPED, method?.userId)
        }
        val wasDefault = method.isDefault
        val notify = detach(method, wasDefault)
        method.concealDeletedKey(now)
        return BillingKeyDeletionApplied(BillingKeyDeletionOutcome.DELETED, method.userId, notify)
    }

    private fun reflect(
        method: TossBillingMethod,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingKeyDeletionApplied {
        val outcome = outcomeOf(remote)
        if (outcome == BillingKeyDeletionOutcome.DELETED) {
            method.concealDeletedKey(now)
        }
        if (outcome == BillingKeyDeletionOutcome.FAILED) {
            method.markDeletionFailed(failureMessage(remote), now)
        }
        return BillingKeyDeletionApplied(outcome, method.userId)
    }

    private fun detach(
        method: TossBillingMethod,
        wasDefault: Boolean,
    ): Boolean {
        val subscription = subscriptionRepository.findByUserIdAndPlanCode(method.userId, PLAN_MONTHLY)
        val owns = subscription != null && subscription.billingMethod.id == method.id
        val next = if (wasDefault || owns) nextActive(method.userId, method.id) else null
        if (next != null) {
            next.markAsDefault()
        }
        if (owns && next != null && subscription != null) {
            subscription.billingMethod = next
        }
        val pause = owns && next == null && subscription?.status == TossBillingSubscriptionStatus.ACTIVE
        if (owns && next == null && subscription != null) {
            subscription.retryAt = null
            if (pause) {
                subscription.status = TossBillingSubscriptionStatus.PAUSED
            }
        }
        return pause
    }

    private fun nextActive(
        userId: Long,
        excludedId: Long,
    ): TossBillingMethod? =
        methodRepository
            .findAllByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(userId)
            .firstOrNull { it.id != excludedId && it.usableForCharge() }

    private fun prepared(
        method: TossBillingMethod,
        callToss: Boolean,
        notify: Boolean,
    ): BillingKeyDeletionPrepared =
        BillingKeyDeletionPrepared(
            methodId = method.id,
            userId = method.userId,
            billingKey = if (callToss) method.billingKey else null,
            callToss = callToss,
            notify = notify,
        )

    private fun outcomeOf(remote: TossApiResponse): BillingKeyDeletionOutcome =
        when {
            remote.isSuccess2xx || remote.httpStatus == HTTP_NOT_FOUND -> BillingKeyDeletionOutcome.DELETED
            inconclusive(remote) -> BillingKeyDeletionOutcome.PENDING
            else -> BillingKeyDeletionOutcome.FAILED
        }

    private fun inconclusive(remote: TossApiResponse): Boolean =
        remote.httpStatus == 0 ||
            remote.httpStatus >= HTTP_SERVER_ERROR ||
            remote.httpStatus == HTTP_CONFLICT

    private fun failureMessage(remote: TossApiResponse): String =
        remote.json?.path("message")?.asText(null)
            ?: "토스 빌링키 삭제가 거절되었습니다. httpStatus=${remote.httpStatus}"

    private companion object {
        const val PLAN_MONTHLY = "MONTHLY"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_SERVER_ERROR = 500
    }
}
