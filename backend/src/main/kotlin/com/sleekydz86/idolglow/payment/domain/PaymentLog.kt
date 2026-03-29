package com.sleekydz86.idolglow.payment.domain

import com.sleekydz86.idolglow.global.BaseEntity
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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "payment_logs")
class PaymentLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    val payment: Payment? = null,

    @Column(name = "order_id", length = 200)
    val orderId: String? = null,

    @Column(name = "payment_key", length = 200)
    val paymentKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 50)
    val logType: PaymentLogType,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val step: PaymentLogStep? = null,

    @Column(name = "request_url", length = 500)
    val requestUrl: String? = null,

    @Column(name = "http_method", length = 20)
    val httpMethod: String? = null,

    @Column(name = "http_status")
    val httpStatus: Int? = null,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "request_body")
    val requestBody: String? = null,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "response_body")
    val responseBody: String? = null,

    @Column(name = "error_code", length = 100)
    val errorCode: String? = null,

    @Column(name = "error_message", length = 1000)
    val errorMessage: String? = null,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "stack_trace")
    val stackTrace: String? = null,

    @Column(name = "client_ip", length = 100)
    val clientIp: String? = null,

    @Column(name = "user_agent", length = 500)
    val userAgent: String? = null,

    @Column(name = "trace_id", length = 100)
    val traceId: String? = null,
) : BaseEntity()
