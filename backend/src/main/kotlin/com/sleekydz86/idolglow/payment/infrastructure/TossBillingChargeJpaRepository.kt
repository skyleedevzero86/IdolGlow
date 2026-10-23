package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TossBillingChargeJpaRepository : JpaRepository<TossBillingCharge, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<TossBillingCharge>

    fun findByOrderId(orderId: String): TossBillingCharge?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TossBillingCharge c where c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): TossBillingCharge?
}
