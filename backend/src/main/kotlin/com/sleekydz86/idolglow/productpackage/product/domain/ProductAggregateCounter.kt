package com.sleekydz86.idolglow.productpackage.product.domain

interface ProductAggregateCounter {
    fun changeWishCount(
        productId: Long,
        delta: Long,
    )

    fun changeReviewAggregate(
        productId: Long,
        reviewCountDelta: Long,
        ratingSumDelta: Long,
    )

    fun decreaseWishCountsOfUser(userId: Long)
}
