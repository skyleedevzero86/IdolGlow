package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import org.springframework.data.jpa.repository.JpaRepository

interface TossBillingChargeAttemptJpaRepository : JpaRepository<TossBillingChargeAttempt, Long> {
    fun findAllByChargeIdOrderByAttemptNoAsc(chargeId: Long): List<TossBillingChargeAttempt>

    fun findTopByChargeIdOrderByAttemptNoDesc(chargeId: Long): TossBillingChargeAttempt?

    fun findAllByChargeIdInOrderByChargeIdAscAttemptNoAsc(chargeIds: Collection<Long>): List<TossBillingChargeAttempt>
}
