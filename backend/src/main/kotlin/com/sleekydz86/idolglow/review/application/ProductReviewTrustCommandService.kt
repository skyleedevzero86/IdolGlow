package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.review.domain.ProductReviewHelpfulVote
import com.sleekydz86.idolglow.review.domain.ProductReviewReport
import com.sleekydz86.idolglow.review.domain.ProductReviewRepository
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewHelpfulVoteJpaRepository
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewReportJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Transactional
@Service
class ProductReviewTrustCommandService(
    private val productReviewRepository: ProductReviewRepository,
    private val helpfulVoteJpaRepository: ProductReviewHelpfulVoteJpaRepository,
    private val reportJpaRepository: ProductReviewReportJpaRepository,
) {

    fun toggleHelpful(productId: Long, reviewId: Long, userId: Long): Long {
        val review = productReviewRepository.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다.")
        require(review.product.id == productId) { "상품과 리뷰가 일치하지 않습니다." }
        require(review.userId != userId) { "본인 리뷰에는 도움돼요를 누를 수 없습니다." }
        require(!review.isHidden()) { "비공개 처리된 리뷰입니다." }

        val existing = helpfulVoteJpaRepository.findByReviewIdAndUserId(reviewId, userId)
        if (existing != null) {
            helpfulVoteJpaRepository.delete(existing)
            review.helpfulCount = (review.helpfulCount - 1).coerceAtLeast(0L)
        } else {
            helpfulVoteJpaRepository.save(
                ProductReviewHelpfulVote(
                    review = review,
                    userId = userId,
                )
            )
            review.helpfulCount += 1
        }
        productReviewRepository.save(review)
        return review.helpfulCount
    }

    fun reportReview(productId: Long, reviewId: Long, reporterUserId: Long, reason: String) {
        val trimmed = reason.trim()
        require(trimmed.length in 1..200) { "신고 사유는 1~200자여야 합니다." }

        val review = productReviewRepository.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다.")
        require(review.product.id == productId) { "상품과 리뷰가 일치하지 않습니다." }
        require(review.userId != reporterUserId) { "본인 리뷰는 신고할 수 없습니다." }
        require(!reportJpaRepository.existsByReview_IdAndReporterUserId(reviewId, reporterUserId)) {
            "이미 신고한 리뷰입니다."
        }

        reportJpaRepository.save(
            ProductReviewReport(
                review = review,
                reporterUserId = reporterUserId,
                reason = trimmed,
            )
        )

        val total = reportJpaRepository.countByReviewId(reviewId)
        if (total >= REPORTS_TO_AUTO_HIDE && !review.isHidden()) {
            review.hide(
                LocalDateTime.now(ZoneOffset.UTC),
                "신고 누적 자동 비공개"
            )
            productReviewRepository.save(review)
        }
    }

    companion object {
        private const val REPORTS_TO_AUTO_HIDE = 3L
    }
}
