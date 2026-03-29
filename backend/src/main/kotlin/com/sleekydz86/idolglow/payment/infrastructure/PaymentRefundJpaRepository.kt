package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRefundJpaRepository : JpaRepository<PaymentRefund, Long> {
    fun findAllByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentRefund>
}
