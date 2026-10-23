package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TossBillingChargeAttemptJpaRepository : JpaRepository<TossBillingChargeAttempt, Long> {
    fun findAllByChargeIdOrderByAttemptNoAsc(chargeId: Long): List<TossBillingChargeAttempt>

    fun findTopByChargeIdOrderByAttemptNoDesc(chargeId: Long): TossBillingChargeAttempt?

    @Query("select a.chargeId from TossBillingChargeAttempt a where a.id = :id")
    fun findChargeIdById(
        @Param("id") id: Long,
    ): Long?

    fun findAllByChargeIdInOrderByChargeIdAscAttemptNoAsc(chargeIds: Collection<Long>): List<TossBillingChargeAttempt>
}
