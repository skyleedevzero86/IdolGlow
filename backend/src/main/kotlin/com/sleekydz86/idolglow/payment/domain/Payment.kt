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
import java.util.UUID

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_payments_reservation_id", columnNames = ["reservation_id"]),
        UniqueConstraint(name = "uk_payments_reference", columnNames = ["payment_reference"]),
        UniqueConstraint(name = "uk_payments_payment_no", columnNames = ["payment_no"]),
        UniqueConstraint(name = "uk_payments_order_id", columnNames = ["order_id"]),
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

    @Column(name = "payment_no", nullable = false, length = 64)
    val paymentNo: String,

    @Column(name = "order_id", nullable = false, length = 200)
    val orderId: String,

    @Column(name = "payment_key", length = 200)
    var paymentKey: String? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "order_name", length = 255)
    var orderName: String? = null,

    @Column(name = "supplied_amount", precision = 15, scale = 2)
    var suppliedAmount: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var vat: BigDecimal? = null,

    @Column(name = "tax_free_amount", precision = 15, scale = 2)
    var taxFreeAmount: BigDecimal? = null,

    @Column(length = 10)
    var currency: String? = null,

    @Column(name = "gateway_method", length = 50)
    var gatewayMethod: String? = null,

    @Column(name = "gateway_type", length = 50)
    var gatewayType: String? = null,

    @Column(name = "external_status", length = 50)
    var externalStatus: String? = null,

    @Column(name = "requested_at")
    var requestedAt: LocalDateTime? = null,

    @Column(name = "last_transaction_key", length = 200)
    var lastTransactionKey: String? = null,

    @Column(name = "fail_code", length = 100)
    var failCode: String? = null,

    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "failed_at")
    var failedAt: LocalDateTime? = null,

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null,

    @Column(name = "failure_reason", length = 255)
    var failureReason: String? = null,

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null,

    @Column(name = "cancel_amount", nullable = false, precision = 15, scale = 2)
    var cancelAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "card_company", length = 100)
    var cardCompany: String? = null,

    @Column(name = "card_number", length = 50)
    var cardNumber: String? = null,

    @Column(name = "installment_plan_months")
    var installmentPlanMonths: Int? = null,

    @Column(name = "is_interest_free")
    var interestFree: Boolean? = null,

    @Column(name = "virtual_account_bank", length = 100)
    var virtualAccountBank: String? = null,

    @Column(name = "virtual_account_number", length = 100)
    var virtualAccountNumber: String? = null,

    @Column(name = "virtual_account_due_date")
    var virtualAccountDueDate: LocalDateTime? = null,

    @Column(name = "easy_pay_provider", length = 100)
    var easyPayProvider: String? = null,

    @jakarta.persistence.Lob
    @Column(name = "raw_response_json")
    var rawResponseJson: String? = null,

    @Column(name = "idempotency_key", length = 100)
    var idempotencyKey: String? = null,
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
        failCode = null
    }

    fun markFailed(reason: String, failedAt: LocalDateTime = LocalDateTime.now(), code: String? = null) {
        if (status == PaymentStatus.FAILED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 실패 처리할 수 있습니다." }
        status = PaymentStatus.FAILED
        this.failedAt = failedAt
        approvedAt = null
        expiredAt = null
        failCode = code?.trim()?.takeIf { it.isNotBlank() }
        failureReason = reason.trim().takeIf { it.isNotBlank() }
    }

    fun markCanceled(reason: String? = null) {
        if (status == PaymentStatus.CANCELED) {
            return
        }
        require(status == PaymentStatus.PENDING) { "대기 중인 결제만 취소할 수 있습니다." }
        status = PaymentStatus.CANCELED
        failureReason = reason?.trim()?.takeIf { it.isNotBlank() }
        failCode = null
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
        failCode = null
        failureReason = "결제 시간이 만료되었습니다."
    }

    fun markFullRefund(canceledAt: LocalDateTime = LocalDateTime.now()) {
        require(status == PaymentStatus.SUCCEEDED || status == PaymentStatus.PARTIAL_CANCELED) {
            "승인된 결제만 전액 환불 처리할 수 있습니다."
        }
        status = PaymentStatus.REFUNDED
        this.canceledAt = canceledAt
        cancelAmount = amount
    }

    fun markPartialRefund(additionalCancelAmount: BigDecimal, canceledAt: LocalDateTime = LocalDateTime.now()) {
        require(additionalCancelAmount > BigDecimal.ZERO) { "취소 금액이 있어야 합니다." }
        require(status == PaymentStatus.SUCCEEDED || status == PaymentStatus.PARTIAL_CANCELED) {
            "승인된 결제만 부분 취소할 수 있습니다."
        }
        val next = cancelAmount.add(additionalCancelAmount)
        require(next <= amount) { "취소 합계가 결제 금액을 초과할 수 없습니다." }
        cancelAmount = next
        this.canceledAt = canceledAt
        status = if (next >= amount) PaymentStatus.REFUNDED else PaymentStatus.PARTIAL_CANCELED
    }

    companion object {
        fun createMock(
            reservation: Reservation,
            paymentReference: String,
        ): Payment {
            val paymentNo = "PN_${UUID.randomUUID().toString().replace("-", "")}"
            return Payment(
                reservation = reservation,
                provider = PaymentProvider.MOCK,
                paymentReference = paymentReference,
                paymentNo = paymentNo,
                orderId = paymentReference,
                amount = reservation.totalPrice,
                status = PaymentStatus.PENDING,
            )
        }

        fun createPendingForToss(
            reservation: Reservation,
            paymentReference: String,
        ): Payment {
            val orderId = "ORD_${UUID.randomUUID().toString().replace("-", "")}"
            val paymentNo = "PN_${UUID.randomUUID().toString().replace("-", "")}"
            return Payment(
                reservation = reservation,
                provider = PaymentProvider.TOSS,
                paymentReference = paymentReference,
                paymentNo = paymentNo,
                orderId = orderId,
                amount = reservation.totalPrice,
                status = PaymentStatus.PENDING,
            )
        }
    }
}
