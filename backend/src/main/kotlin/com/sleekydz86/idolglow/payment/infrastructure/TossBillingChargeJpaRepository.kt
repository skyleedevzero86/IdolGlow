package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import org.springframework.data.jpa.repository.JpaRepository

interface TossBillingChargeJpaRepository : JpaRepository<TossBillingCharge, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<TossBillingCharge>

    fun findByOrderId(orderId: String): TossBillingCharge?
}
