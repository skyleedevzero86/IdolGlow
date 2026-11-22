package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientCounts
import org.springframework.stereotype.Repository

@Repository
class SubscriptionDispatchRecipientRepositoryImpl(
    private val subscriptionDispatchRecipientJpaRepository: SubscriptionDispatchRecipientJpaRepository,
) : SubscriptionDispatchRecipientPort {
    override fun save(recipient: SubscriptionDispatchRecipient): SubscriptionDispatchRecipient =
        subscriptionDispatchRecipientJpaRepository.saveAndFlush(recipient)

    override fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient> =
        subscriptionDispatchRecipientJpaRepository.findByDispatchId(dispatchId)

    override fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient? =
        subscriptionDispatchRecipientJpaRepository.findByDispatchIdAndEmail(dispatchId, email)

    override fun countByDispatchIds(dispatchIds: Collection<Long>): Map<Long, SubscriptionDispatchRecipientCounts> {
        if (dispatchIds.isEmpty()) return emptyMap()
        return subscriptionDispatchRecipientJpaRepository
            .countByDispatchIdsGroupByStatus(dispatchIds.toSet())
            .groupBy { it.dispatchId }
            .mapValues { (_, rows) ->
                rows.fold(SubscriptionDispatchRecipientCounts.EMPTY) { counts, row ->
                    counts.plus(row.status, row.total)
                }
            }
    }
}
