package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
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
}
