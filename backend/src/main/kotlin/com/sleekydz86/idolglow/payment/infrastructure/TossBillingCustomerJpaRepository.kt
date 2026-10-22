package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingCustomer
import org.springframework.data.jpa.repository.JpaRepository

interface TossBillingCustomerJpaRepository : JpaRepository<TossBillingCustomer, Long> {
    fun findByUserId(userId: Long): TossBillingCustomer?
}
