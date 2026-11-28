package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "toss_billing_plan_changes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_toss_billing_plan_changes_period_cycle",
            columnNames = ["subscription_id", "period_end_at", "to_cycle_months"],
        ),
    ],
)
class TossBillingPlanChange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "subscription_id", nullable = false)
    val subscriptionId: Long,
    @Column(name = "from_cycle_months", nullable = false)
    val fromCycleMonths: Int,
    @Column(name = "to_cycle_months", nullable = false)
    val toCycleMonths: Int,
    @Column(name = "period_end_at", nullable = false)
    val periodEndAt: LocalDateTime,
    quote: PlanChangeQuote,
) : BaseEntity() {
    @Column(name = "period_days", nullable = false)
    var periodDays: Int = quote.periodDays
        private set

    @Column(name = "remaining_days", nullable = false)
    var remainingDays: Int = quote.remainingDays
        private set

    @Column(name = "remaining_value", nullable = false)
    var remainingValue: Long = quote.remainingValue
        private set

    @Column(name = "new_amount", nullable = false)
    var newAmount: Long = quote.newAmount
        private set

    @Column(name = "credit_used", nullable = false)
    var creditUsed: Long = quote.creditUsed
        private set

    @Column(name = "credit_added", nullable = false)
    var creditAdded: Long = quote.creditAdded
        private set

    @Column(name = "card_amount", nullable = false)
    var cardAmount: Long = quote.cardAmount
        private set

    @Column(name = "charge_id")
    var chargeId: Long? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TossBillingPlanChangeStatus = TossBillingPlanChangeStatus.PENDING
        private set

    fun restart(quote: PlanChangeQuote) {
        require(status == TossBillingPlanChangeStatus.FAILED) {
            "실패한 이용권 변경만 다시 시도할 수 있습니다. status=$status"
        }
        periodDays = quote.periodDays
        remainingDays = quote.remainingDays
        remainingValue = quote.remainingValue
        newAmount = quote.newAmount
        creditUsed = quote.creditUsed
        creditAdded = quote.creditAdded
        cardAmount = quote.cardAmount
        chargeId = null
        status = TossBillingPlanChangeStatus.PENDING
    }

    fun attachCharge(id: Long) {
        require(status == TossBillingPlanChangeStatus.PENDING) {
            "대기 중인 이용권 변경에만 결제를 연결할 수 있습니다. status=$status"
        }
        chargeId = id
    }

    fun markDone() {
        require(status == TossBillingPlanChangeStatus.PENDING) {
            "대기 중인 이용권 변경만 완료할 수 있습니다. status=$status"
        }
        status = TossBillingPlanChangeStatus.DONE
    }

    fun markFailed() {
        require(status == TossBillingPlanChangeStatus.PENDING) {
            "대기 중인 이용권 변경만 실패로 바꿀 수 있습니다. status=$status"
        }
        status = TossBillingPlanChangeStatus.FAILED
    }
}
