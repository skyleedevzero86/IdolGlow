package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.RenewalNoticeResponse
import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingRenewalNoticeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

data class PreparedRenewalNotice(
    val noticeId: Long,
    val token: String,
    val userId: Long,
    val periodEnd: LocalDateTime,
    val amount: Long,
    val cycleMonths: Int,
    val autoRenew: Boolean,
)

@Service
class BillingRenewalNoticeTxService(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val noticeRepository: TossBillingRenewalNoticeJpaRepository,
) {
    private val random = SecureRandom()

    @Transactional(readOnly = true)
    fun findTargetIds(
        now: LocalDateTime,
        daysBefore: Long,
    ): List<Long> =
        subscriptionRepository.findRenewalNoticeTargetIds(
            statuses = NOTICE_STATUSES,
            now = now,
            until = now.plusDays(daysBefore),
        )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepare(subscriptionId: Long): PreparedRenewalNotice? {
        val subscription =
            subscriptionRepository
                .findById(subscriptionId)
                .orElse(null)
                ?.takeIf { it.status in NOTICE_STATUSES && it.nextBillingAt != null }
        val periodEnd = subscription?.nextBillingAt
        if (subscription == null ||
            periodEnd == null ||
            noticeRepository.existsBySubscriptionIdAndPeriodEnd(subscriptionId, periodEnd)
        ) {
            return null
        }
        val token = newToken()
        val notice =
            noticeRepository.saveAndFlush(
                TossBillingRenewalNotice(
                    subscriptionId = subscriptionId,
                    userId = subscription.userId,
                    periodEnd = periodEnd,
                    tokenHash = BillingKeyCrypto.sha256Hex(token),
                ),
            )
        return PreparedRenewalNotice(
            noticeId = notice.id,
            token = token,
            userId = subscription.userId,
            periodEnd = periodEnd,
            amount = subscription.amount,
            cycleMonths = subscription.cycleMonths,
            autoRenew = subscription.autoRenew,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markSent(
        noticeId: Long,
        now: LocalDateTime,
    ) {
        noticeRepository.findById(noticeId).ifPresent { it.markSent(now) }
    }

    @Transactional(readOnly = true)
    fun preview(
        token: String,
        now: LocalDateTime,
    ): RenewalNoticeResponse {
        val notice = findNotice(token)
        return RenewalNoticeResponse.of(notice, findSubscription(notice), now)
    }

    @Transactional
    fun decide(
        token: String,
        decision: RenewalDecision,
        now: LocalDateTime,
    ): RenewalNoticeResponse {
        val notice = findNotice(token)
        val subscription = findSubscription(notice)
        require(subscription.nextBillingAt == notice.periodEnd) {
            "이미 다음 이용 기간으로 넘어간 안내입니다. periodEnd=${notice.periodEnd}"
        }
        notice.decide(decision, now)
        when (decision) {
            RenewalDecision.CONTINUE -> continueSubscription(subscription, now)
            RenewalDecision.STOP -> stopSubscription(subscription, now)
        }
        return RenewalNoticeResponse.of(notice, subscription, now)
    }

    private fun continueSubscription(
        subscription: TossBillingSubscription,
        now: LocalDateTime,
    ) {
        if (subscription.status == TossBillingSubscriptionStatus.CANCEL_SCHEDULED) {
            subscription.withdrawCancel(now)
        } else {
            subscription.autoRenew = true
        }
    }

    private fun stopSubscription(
        subscription: TossBillingSubscription,
        now: LocalDateTime,
    ) {
        if (subscription.status != TossBillingSubscriptionStatus.CANCEL_SCHEDULED) {
            subscription.scheduleCancel(now)
        }
    }

    private fun findNotice(token: String): TossBillingRenewalNotice =
        noticeRepository.findByTokenHash(BillingKeyCrypto.sha256Hex(token.trim()))
            ?: throw IllegalArgumentException("안내 링크가 올바르지 않습니다.")

    private fun findSubscription(notice: TossBillingRenewalNotice): TossBillingSubscription =
        subscriptionRepository.findById(notice.subscriptionId).orElseThrow {
            IllegalArgumentException("구독을 찾을 수 없습니다. subscriptionId=${notice.subscriptionId}")
        }

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_BYTES = 32
        val NOTICE_STATUSES =
            listOf(
                TossBillingSubscriptionStatus.ACTIVE,
                TossBillingSubscriptionStatus.CANCEL_SCHEDULED,
            )
    }
}
