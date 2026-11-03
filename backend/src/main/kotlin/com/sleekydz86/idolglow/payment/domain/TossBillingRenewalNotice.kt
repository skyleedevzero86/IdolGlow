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
import java.time.LocalDateTime

@Entity
@Table(name = "toss_billing_renewal_notices")
class TossBillingRenewalNotice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "subscription_id", nullable = false)
    val subscriptionId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "period_end", nullable = false)
    val periodEnd: LocalDateTime,
    @Column(name = "token_hash", nullable = false, length = 64)
    val tokenHash: String,
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var decision: RenewalDecision? = null,
    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isExpired(now: LocalDateTime): Boolean = !now.isBefore(periodEnd)

    fun markSent(now: LocalDateTime) {
        sentAt = now
    }

    fun decide(
        choice: RenewalDecision,
        now: LocalDateTime,
    ) {
        require(!isExpired(now)) { "이용 기간이 이미 끝나 선택을 바꿀 수 없습니다. periodEnd=$periodEnd" }
        decision = choice
        decidedAt = now
    }
}
