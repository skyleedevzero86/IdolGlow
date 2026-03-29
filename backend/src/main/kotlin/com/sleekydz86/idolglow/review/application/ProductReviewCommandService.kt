package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.review.application.dto.CreateProductReviewCommand
import com.sleekydz86.idolglow.review.application.dto.ReviewImageFile
import com.sleekydz86.idolglow.review.application.dto.UpdateProductReviewCommand
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.domain.ProductReviewRepository
import com.sleekydz86.idolglow.review.domain.ReviewRating
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Transactional
@Service
class ProductReviewCommandService(
    private val productRepository: ProductRepository,
    private val productReviewRepository: ProductReviewRepository,
    private val reservationRepository: ReservationRepository,
    private val imageEventPublisher: ImageEventPublisher,
) {

    fun createReview(
        command: CreateProductReviewCommand,
        images: List<ReviewImageFile> = emptyList()
    ): ProductReview {
        val product = loadProduct(command.productId)
        ensureNotReviewed(product.id, command.userId)

        val reservation = loadReservationForReview(
            reservationId = command.reservationId,
            productId = command.productId,
            userId = command.userId,
        )
        require(!productReviewRepository.existsByReservationId(reservation.id)) {
            "이 예약으로 이미 리뷰가 등록되었습니다."
        }

        val review = ProductReview(
            product = product,
            userId = command.userId,
            rating = ReviewRating.of(command.rating),
            content = command.content,
            reservationId = reservation.id,
        )

        val productReview = productReviewRepository.save(review)
        storeImages(productReview.id, images)
        return productReview
    }

    private fun loadReservationForReview(reservationId: Long, productId: Long, userId: Long): Reservation {
        val r = reservationRepository.findByIdWithSlotAndProduct(reservationId)
            ?: throw EntityNotFoundException("예약을 찾을 수 없습니다: $reservationId")
        val today = LocalDate.now(ZoneOffset.UTC)
        r.validateOwner(userId)
        require(r.reservationSlot.product.id == productId) { "예약이 해당 상품과 일치하지 않습니다." }
        require(r.status == ReservationStatus.BOOKED) { "확정된 예약만 리뷰를 작성할 수 있습니다." }
        require(r.visitDate.isBefore(today)) { "방문일 이후에만 리뷰를 작성할 수 있습니다." }
        return r
    }

    private fun loadProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw EntityNotFoundException("상품을 찾을 수 없습니다: $productId")
    }

    private fun ensureNotReviewed(productId: Long, userId: Long) {
        val existing = productReviewRepository.findByProductIdAndUserId(productId, userId)
        require(existing == null) { "이 상품에는 이미 리뷰를 작성했습니다." }
    }

    fun updateReview(
        command: UpdateProductReviewCommand,
        images: List<ReviewImageFile>? = null
    ): ProductReview {
        val review = findProductByReviewId(command.reviewId)
        review.validateOwner(userId = command.userId, productId = command.productId)
        review.changeReview(
            rating = ReviewRating.of(command.rating),
            content = command.content
        )

        images?.let {
            deleteImages(review.id)
            storeImages(review.id, it)
        }
        return review
    }

    private fun findProductByReviewId(reviewId: Long) =
        productReviewRepository.findById(reviewId)
            ?: throw IllegalArgumentException("리뷰를 찾을 수 없습니다: $reviewId")

    fun deleteReview(productId: Long, reviewId: Long, userId: Long) {
        val review = findProductByReviewId(reviewId)
        review.validateOwner(userId = userId, productId = productId)
        deleteImages(reviewId)
        productReviewRepository.delete(review)
    }

    private fun storeImages(reviewId: Long, images: List<ReviewImageFile>) {
        images.forEach { image ->
            imageEventPublisher.publishCreate(
                aggregateType = ImageAggregateType.PRODUCT_REVIEW,
                aggregateId = reviewId,
                originalFilename = image.originalFilename,
                content = image.content,
                sortOrder = image.sortOrder
            )
        }
    }

    private fun deleteImages(reviewId: Long) {
        imageEventPublisher.publishDelete(
            aggregateType = ImageAggregateType.PRODUCT_REVIEW,
            aggregateId = reviewId,
        )
    }
}
