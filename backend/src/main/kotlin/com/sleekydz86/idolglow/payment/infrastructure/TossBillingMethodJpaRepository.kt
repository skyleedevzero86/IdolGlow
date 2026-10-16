package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import org.springframework.data.jpa.repository.JpaRepository

interface TossBillingMethodJpaRepository : JpaRepository<TossBillingMethod, Long> {
    fun findByUserIdAndActiveTrueAndIsDefaultTrue(userId: Long): TossBillingMethod?

    fun findAllByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(userId: Long): List<TossBillingMethod>

    fun findAllByUserIdOrderByIdDesc(userId: Long): List<TossBillingMethod>

    fun findByIdAndUserIdAndActiveTrue(
        id: Long,
        userId: Long,
    ): TossBillingMethod?
}
