package com.sleekydz86.idolglow.wish.application

import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.wish.application.event.WishDeleteEvent
import com.sleekydz86.idolglow.wish.domain.WishRepository
import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WishEventListener(
    private val wishRepository: WishRepository,
    private val productAggregateCounter: ProductAggregateCounter,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleWishDelete(event: WishDeleteEvent) {
        when (event.aggregateType) {
            WishAggregateType.USER -> {
                productAggregateCounter.decreaseWishCountsOfUser(event.aggregateId)
                wishRepository.deleteAllByUserId(event.aggregateId)
            }
            WishAggregateType.PRODUCT -> wishRepository.deleteAllByProductId(event.aggregateId)
        }
    }
}
