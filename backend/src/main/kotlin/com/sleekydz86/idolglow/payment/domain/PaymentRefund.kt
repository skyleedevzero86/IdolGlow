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
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "payment_refunds")
class PaymentRefund(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    val payment: Payment,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: Reservation,

    @Column(name = "cancel_amount", nullable = false, precision = 15, scale = 2)
    val cancelAmount: BigDecimal,

    @Column(name = "cancel_reason", nullable = false, length = 500)
    val cancelReason: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PaymentRefundStatus = PaymentRefundStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false, length = 20)
    val requestedBy: RefundRequestedBy,

    @Column(name = "external_transaction_key", length = 200)
    var externalTransactionKey: String? = null,

    @Column(name = "fail_code", length = 100)
    var failCode: String? = null,

    @Column(name = "fail_message", length = 500)
    var failMessage: String? = null,

    @Column(name = "idempotency_key", length = 100)
    val idempotencyKey: String? = null,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "raw_response_json")
    var rawResponseJson: String? = null,
) : BaseEntity()
