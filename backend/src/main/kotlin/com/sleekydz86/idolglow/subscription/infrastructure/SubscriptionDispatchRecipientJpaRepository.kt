package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface SubscriptionDispatchRecipientJpaRepository :
    JpaRepository<SubscriptionDispatchRecipient, Long>,
    JpaSpecificationExecutor<SubscriptionDispatchRecipient> {
    fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient>

    fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient?

    @Query(
        """
        select r.dispatchId as dispatchId, r.status as status, count(r) as total
          from SubscriptionDispatchRecipient r
         where r.dispatchId in :dispatchIds
         group by r.dispatchId, r.status
        """,
    )
    fun countByDispatchIdsGroupByStatus(dispatchIds: Collection<Long>): List<SubscriptionDispatchRecipientStatusCount>
}
