package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminProductReviewPageResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminProductReviewSummaryResponse
import com.sleekydz86.idolglow.image.domain.ImageRepository
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.review.application.dto.ProductReviewImageResponse
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class AdminProductReviewService(
    private val productReviewJpaRepository: ProductReviewJpaRepository,
    private val imageRepository: ImageRepository,
    private val adminAuditService: AdminAuditService,
) {

    fun findReviews(
        keyword: String?,
        visibility: AdminReviewVisibilityFilter,
        page: Int,
        size: Int,
    ): AdminProductReviewPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 50)
        val spec = buildSpec(keyword, visibility)
        val pageable = PageRequest.of(
            resolvedPage - 1,
            resolvedSize,
            Sort.by(Sort.Direction.DESC, "createdAt"),
        )
        val result = productReviewJpaRepository.findAll(spec, pageable)
        val reviewIds = result.content.map { it.id }
        val images = imageRepository.findByAggregates(ImageAggregateType.PRODUCT_REVIEW, reviewIds)
        val imageMap = images.groupBy { it.aggregateId }

        val items = result.content.map { review ->
            toSummary(
                review,
                imageMap[review.id].orEmpty().map { ProductReviewImageResponse.from(it) },
            )
        }

        val totalPages = when {
            result.totalElements == 0L -> 1
            else -> result.totalPages.coerceAtLeast(1)
        }

        return AdminProductReviewPageResponse(
            reviews = items,
            page = resolvedPage,
            size = resolvedSize,
            totalElements = result.totalElements,
            totalPages = totalPages,
            hasNext = result.hasNext(),
        )
    }

    @Transactional
    fun hideReview(reviewId: Long, reason: String?): AdminProductReviewSummaryResponse {
        val review = productReviewJpaRepository.findById(reviewId)
            .orElseThrow { EntityNotFoundException("리뷰를 찾을 수 없습니다. reviewId=$reviewId") }
        val trimmed = reason?.trim().orEmpty()
        val resolved = trimmed.ifBlank { "관리자 비공개 처리" }
        review.hide(LocalDateTime.now(ZoneOffset.UTC), resolved)
        val saved = productReviewJpaRepository.save(review)
        adminAuditService.log("REVIEW_HIDE", "PRODUCT_REVIEW", saved.id, "reason=${saved.hiddenReason}")
        return toSummaryResponse(saved)
    }

    @Transactional
    fun unhideReview(reviewId: Long): AdminProductReviewSummaryResponse {
        val review = productReviewJpaRepository.findById(reviewId)
            .orElseThrow { EntityNotFoundException("리뷰를 찾을 수 없습니다. reviewId=$reviewId") }
        review.unhide()
        val saved = productReviewJpaRepository.save(review)
        adminAuditService.log("REVIEW_UNHIDE", "PRODUCT_REVIEW", saved.id, null)
        return toSummaryResponse(saved)
    }

    private fun toSummaryResponse(review: ProductReview): AdminProductReviewSummaryResponse {
        val imgs = imageRepository.findByAggregate(ImageAggregateType.PRODUCT_REVIEW, review.id)
            .map { ProductReviewImageResponse.from(it) }
        return toSummary(review, imgs)
    }

    private fun toSummary(
        review: ProductReview,
        images: List<ProductReviewImageResponse>,
    ): AdminProductReviewSummaryResponse {
        val createdAt = review.createdAt ?: LocalDateTime.now()
        return AdminProductReviewSummaryResponse(
            reviewId = review.id,
            productId = review.product.id,
            productName = review.product.name,
            userId = review.userId,
            rating = review.rating.value,
            content = review.content,
            createdAt = createdAt,
            hidden = review.isHidden(),
            hiddenReason = review.hiddenReason,
            helpfulCount = review.helpfulCount,
            images = images,
        )
    }

    private fun buildSpec(
        keyword: String?,
        visibility: AdminReviewVisibilityFilter,
    ): Specification<ProductReview> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            when (visibility) {
                AdminReviewVisibilityFilter.VISIBLE ->
                    predicates.add(cb.isNull(root.get<LocalDateTime>("hiddenAt")))
                AdminReviewVisibilityFilter.HIDDEN ->
                    predicates.add(cb.isNotNull(root.get<LocalDateTime>("hiddenAt")))
                AdminReviewVisibilityFilter.ALL -> Unit
            }

            val trimmed = keyword?.trim().orEmpty()
            if (trimmed.isNotEmpty()) {
                val productJoin = root.join<ProductReview, Product>("product", JoinType.INNER)
                val like = "%${trimmed.lowercase()}%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("content")), like),
                        cb.like(cb.lower(productJoin.get<String>("name")), like),
                    ),
                )
            }

            if (predicates.isEmpty()) {
                cb.conjunction()
            } else {
                cb.and(*predicates.toTypedArray())
            }
        }
}
