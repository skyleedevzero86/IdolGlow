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
@Table(name = "toss_billing_charges")
class TossBillingCharge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "subscription_id")
    var subscriptionId: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_method_id", nullable = false)
    var billingMethod: TossBillingMethod,
    @Column(name = "order_id", nullable = false, length = 200)
    val orderId: String,
    @Column(name = "order_name", nullable = false, length = 200)
    val orderName: String,
    @Column(nullable = false)
    val amount: Long,
    @Column(name = "payment_key", length = 200)
    var paymentKey: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TossBillingChargeStatus = TossBillingChargeStatus.PENDING,
    @Column(name = "raw_response", columnDefinition = "TEXT")
    var rawResponse: String? = null,
    @Column(name = "fail_message", length = 500)
    var failMessage: String? = null,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun markDone(
        paymentKey: String?,
        raw: String?,
        approvedAt: LocalDateTime = LocalDateTime.now(),
    ) {
        this.paymentKey = paymentKey
        this.rawResponse = raw
        this.status = TossBillingChargeStatus.DONE
        this.approvedAt = approvedAt
        this.failMessage = null
    }

    fun markFailed(
        message: String?,
        raw: String?,
    ) {
        this.status = TossBillingChargeStatus.FAILED
        this.failMessage = message?.take(500)
        this.rawResponse = raw
    }

    fun clearFailureForRetry() {
        this.status = TossBillingChargeStatus.PENDING
        this.failMessage = null
        this.rawResponse = null
        this.paymentKey = null
        this.approvedAt = null
    }
}
