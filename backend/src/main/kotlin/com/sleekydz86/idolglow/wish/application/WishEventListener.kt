package com.sleekydz86.idolglow.wish.application

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WishEventListener(
    private val wishRepository: WishRepository
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleWishDelete(event: WishDeleteEvent) {
        when (event.aggregateType) {
            WishAggregateType.USER -> wishRepository.deleteAllByUserId(event.aggregateId)
            WishAggregateType.PRODUCT -> wishRepository.deleteAllByProductId(event.aggregateId)
        }
    }
}