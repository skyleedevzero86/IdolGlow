package com.sleekydz86.idolglow.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ReviewRating(
    @Column(name = "rating", nullable = false)
    val value: Int = 1
) {

    companion object {
        private const val MIN = 1
        private const val MAX = 5

        fun of(value: Int): ReviewRating {
            require(value in MIN..MAX) { "평점은 $MIN 점 이상 $MAX 점 이하여야 합니다." }
            return ReviewRating(value)
        }
    }
}
