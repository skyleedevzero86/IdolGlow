package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import org.springframework.data.jpa.repository.JpaRepository

interface TossBillingMethodJpaRepository : JpaRepository<TossBillingMethod, Long> {
    fun findByUserIdAndActiveTrue(userId: Long): TossBillingMethod?

    fun findAllByUserIdOrderByIdDesc(userId: Long): List<TossBillingMethod>
}
