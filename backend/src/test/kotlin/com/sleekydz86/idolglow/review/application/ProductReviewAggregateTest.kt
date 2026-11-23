package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.review.application.dto.CreateProductReviewCommand
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.domain.ProductReviewRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

@DisplayName("리뷰 작성의 상품 집계 반영")
class ProductReviewAggregateTest {
    private val productRepository = mockk<ProductRepository>()
    private val productReviewRepository = mockk<ProductReviewRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val imageEventPublisher = mockk<ImageEventPublisher>(relaxed = true)
    private val productAggregateCounter = mockk<ProductAggregateCounter>(relaxed = true)
    private val service =
        ProductReviewCommandService(
            productRepository,
            productReviewRepository,
            reservationRepository,
            imageEventPublisher,
            productAggregateCounter,
        )

    @DisplayName("공개 상태로 작성한 리뷰는 그 상품의 공개 리뷰 수를 1, 별점 합계를 별점만큼 늘린다 (E4)")
    @Test
    fun createReviewAddsContribution() {
        // given
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns PRODUCT_ID
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.id } returns RESERVATION_ID
        every { reservation.reservationSlot.product.id } returns PRODUCT_ID
        every { reservation.status } returns ReservationStatus.BOOKED
        every { reservation.visitDate } returns LocalDate.now(ZoneOffset.UTC).minusDays(1)
        every { productRepository.findById(PRODUCT_ID) } returns product
        every { productReviewRepository.findByProductIdAndUserId(PRODUCT_ID, USER_ID) } returns null
        every { reservationRepository.findByIdWithSlotAndProduct(RESERVATION_ID) } returns reservation
        every { productReviewRepository.existsByReservationId(RESERVATION_ID) } returns false
        val saved = slot<ProductReview>()
        every { productReviewRepository.save(capture(saved)) } answers { saved.captured }

        // when
        service.createReview(CreateProductReviewCommand(PRODUCT_ID, USER_ID, RESERVATION_ID, 4, "좋았어요"))

        // then
        verify(exactly = 1) { productAggregateCounter.changeReviewAggregate(PRODUCT_ID, 1L, 4L) }
    }

    companion object {
        private const val PRODUCT_ID = 10L
        private const val USER_ID = 20L
        private const val RESERVATION_ID = 30L
    }
}
