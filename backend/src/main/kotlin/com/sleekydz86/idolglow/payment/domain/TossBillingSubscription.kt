package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "toss_billing_subscriptions")
class TossBillingSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_method_id", nullable = false)
    var billingMethod: TossBillingMethod,
    @Column(name = "plan_code", nullable = false, length = 50)
    val planCode: String = "MONTHLY",
    @Column(name = "order_name", nullable = false, length = 200)
    var orderName: String,
    @Column(nullable = false)
    var amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TossBillingSubscriptionStatus = TossBillingSubscriptionStatus.ACTIVE,
    @Column(name = "next_billing_at")
    var nextBillingAt: LocalDateTime? = null,
    @Column(name = "last_charged_at")
    var lastChargedAt: LocalDateTime? = null,
    @Column(name = "fail_count", nullable = false)
    var failCount: Int = 0,
    @Column(name = "retry_at")
    var retryAt: LocalDateTime? = null,
    @Column(name = "ends_at")
    var endsAt: LocalDateTime? = null,
) : BaseEntity() {
    fun markCharged(now: LocalDateTime = LocalDateTime.now()) {
        val billedPeriodStart = nextBillingAt ?: now
        lastChargedAt = now
        nextBillingAt = billedPeriodStart.plusMonths(1)
        failCount = 0
        retryAt = null
        if (status != TossBillingSubscriptionStatus.CANCEL_SCHEDULED) {
            status = TossBillingSubscriptionStatus.ACTIVE
        }
    }

    fun markChargeFailed(now: LocalDateTime = LocalDateTime.now()) {
        failCount += 1
        when (failCount) {
            1 -> retryAt = now.plusDays(1)
            2 -> retryAt = now.plusDays(2)
            else -> {
                status = TossBillingSubscriptionStatus.PAUSED
                retryAt = null
            }
        }
    }

    fun scheduleCancel(now: LocalDateTime = LocalDateTime.now()) {
        require(status == TossBillingSubscriptionStatus.ACTIVE || status == TossBillingSubscriptionStatus.PAUSED) {
            "해지 예약은 ACTIVE/PAUSED 상태에서만 가능합니다. status=$status"
        }
        status = TossBillingSubscriptionStatus.CANCEL_SCHEDULED
        endsAt = nextBillingAt ?: now
        retryAt = null
    }

    fun withdrawCancel(now: LocalDateTime = LocalDateTime.now()) {
        require(status == TossBillingSubscriptionStatus.CANCEL_SCHEDULED) {
            "해지 철회는 CANCEL_SCHEDULED 상태에서만 가능합니다. status=$status"
        }
        val end = endsAt
        require(end == null || !now.isAfter(end)) {
            "이용 기간이 끝나 해지를 철회할 수 없습니다. endsAt=$end"
        }
        status = TossBillingSubscriptionStatus.ACTIVE
        endsAt = null
    }

    fun finalizeCanceledIfEnded(now: LocalDateTime = LocalDateTime.now()) {
        if (status == TossBillingSubscriptionStatus.CANCEL_SCHEDULED) {
            val end = endsAt
            if (end != null && now.isAfter(end)) {
                status = TossBillingSubscriptionStatus.CANCELED
                nextBillingAt = null
                retryAt = null
            }
        }
    }

    fun isMembershipActive(at: LocalDateTime = LocalDateTime.now()): Boolean {
        finalizeCanceledIfEnded(at)
        return when (status) {
            TossBillingSubscriptionStatus.ACTIVE -> true
            TossBillingSubscriptionStatus.CANCEL_SCHEDULED -> {
                val end = endsAt
                end != null && !at.isAfter(end)
            }
            TossBillingSubscriptionStatus.PAUSED,
            TossBillingSubscriptionStatus.CANCELED,
            -> false
        }
    }

    fun cancelImmediately() {
        status = TossBillingSubscriptionStatus.CANCELED
        nextBillingAt = null
        retryAt = null
        endsAt = null
    }
}
