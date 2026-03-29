package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payments_reservation_id", columnNames = ["reservation_id"]),
        UniqueConstraint(name = "uk_payments_reference", columnNames = ["payment_reference"])
    ]
)
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    val reservation: Reservation,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: PaymentProvider = PaymentProvider.MOCK,

    @Column(name = "payment_reference", nullable = false, length = 80)
    val paymentReference: String,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "failed_at")
    var failedAt: LocalDateTime? = null,

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null,

    @Column(name = "failure_reason", length = 255)
    var failureReason: String? = null,
) : BaseEntity() {

    fun markSucceeded(approvedAt: LocalDateTime = LocalDateTime.now()) {
        if (status == PaymentStatus.SUCCEEDED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 승인할 수 있습니다." }
        status = PaymentStatus.SUCCEEDED
        this.approvedAt = approvedAt
        failedAt = null
        expiredAt = null
        failureReason = null
    }

    fun markFailed(reason: String, failedAt: LocalDateTime = LocalDateTime.now()) {
        if (status == PaymentStatus.FAILED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 실패 처리할 수 있습니다." }
        status = PaymentStatus.FAILED
        this.failedAt = failedAt
        approvedAt = null
        expiredAt = null
        failureReason = reason.trim().takeIf { it.isNotBlank() }
    }

    fun markCanceled(reason: String? = null) {
        if (status == PaymentStatus.CANCELED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 취소할 수 있습니다." }
        status = PaymentStatus.CANCELED
        failureReason = reason?.trim()?.takeIf { it.isNotBlank() }
        approvedAt = null
        failedAt = null
        expiredAt = null
    }

    fun markExpired(expiredAt: LocalDateTime = LocalDateTime.now()) {
        if (status == PaymentStatus.EXPIRED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 만료 처리할 수 있습니다." }
        status = PaymentStatus.EXPIRED
        this.expiredAt = expiredAt
        approvedAt = null
        failedAt = null
        failureReason = "결제 시간이 만료되었습니다."
    }

    companion object {
        fun createMock(
            reservation: Reservation,
            paymentReference: String
        ): Payment = Payment(
            reservation = reservation,
            provider = PaymentProvider.MOCK,
            paymentReference = paymentReference,
            amount = reservation.totalPrice,
            status = PaymentStatus.PENDING
        )
    }
}
