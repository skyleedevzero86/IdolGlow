package com.sleekydz86.idolglow.review.infrastructure

import com.sleekydz86.idolglow.review.domain.ProductReviewReport
import org.springframework.data.jpa.repository.JpaRepository

interface ProductReviewReportJpaRepository : JpaRepository<ProductReviewReport, Long> {
    fun countByReviewId(reviewId: Long): Long

    fun existsByReview_IdAndReporterUserId(reviewId: Long, reporterUserId: Long): Boolean
}
