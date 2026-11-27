package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import org.springframework.stereotype.Component

@Component
class ProductAggregateCounterAdapter(
    private val productJpaRepository: ProductJpaRepository,
) : ProductAggregateCounter {
    override fun changeWishCount(
        productId: Long,
        delta: Long,
    ) {
        if (delta == 0L) return
        val updated = productJpaRepository.addWishCount(productId, delta)
        check(updated == 1) { "찜 수를 바꿀 상품이 없습니다. productId=$productId" }
    }

    override fun changeReviewAggregate(
        productId: Long,
        reviewCountDelta: Long,
        ratingSumDelta: Long,
    ) {
        if (reviewCountDelta == 0L && ratingSumDelta == 0L) return
        val updated = productJpaRepository.addReviewAggregate(productId, reviewCountDelta, ratingSumDelta)
        check(updated == 1) { "리뷰 집계를 바꿀 상품이 없습니다. productId=$productId" }
    }

    override fun decreaseWishCountsOfUser(userId: Long) {
        productJpaRepository.subtractWishCountsOfUser(userId)
    }
}
