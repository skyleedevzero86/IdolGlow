package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.review.domain.ReviewContribution

fun ProductAggregateCounter.applyReviewChange(
    productId: Long,
    before: ReviewContribution,
    after: ReviewContribution,
) {
    val difference = after - before
    changeReviewAggregate(productId, difference.reviewCount, difference.ratingSum)
}
