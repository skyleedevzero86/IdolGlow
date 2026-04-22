package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.PaymentLog
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentLogJpaRepository : JpaRepository<PaymentLog, Long>
{
    fun findAllByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentLog>
}
