package com.sleekydz86.idolglow.productpackage.product.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductEventListener(
    private val productLocationCommandService: ProductLocationCommandService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductLocationAfterProductCreate(event: ProductCreateEvent) {
        event.location?.let {
            productLocationCommandService.upsertProductLocation(event.productId, it)
        }
    }
}