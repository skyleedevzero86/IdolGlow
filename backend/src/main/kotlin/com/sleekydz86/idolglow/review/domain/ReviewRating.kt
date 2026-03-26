package com.sleekydz86.idolglow.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ReviewRating(
    @Column(name = "rating", nullable = false)
    val value: Int
) {

    companion object {
        private const val MIN = 1
        private const val MAX = 5

        fun of(value: Int): ReviewRating {
            require(value in MIN..MAX) { "Rating must be between $MIN and $MAX." }
            return ReviewRating(value)
        }
    }
}