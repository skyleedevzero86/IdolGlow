package com.sleekydz86.idolglow.review.domain

data class ReviewContribution(
    val reviewCount: Long,
    val ratingSum: Long,
) {
    operator fun minus(other: ReviewContribution): ReviewContribution =
        ReviewContribution(reviewCount - other.reviewCount, ratingSum - other.ratingSum)

    companion object {
        val NONE: ReviewContribution = ReviewContribution(0L, 0L)
    }
}
