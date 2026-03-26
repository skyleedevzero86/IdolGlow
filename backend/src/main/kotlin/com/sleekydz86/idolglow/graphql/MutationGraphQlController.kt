package com.sleekydz86.idolglow.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.graphql.ReservationSummaryGraphQlResponse
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import com.sleekydz86.idolglow.review.application.dto.CreateProductReviewCommand
import com.sleekydz86.idolglow.review.graphql.ProductReviewGraphQlResponse
import com.sleekydz86.idolglow.wish.application.WishCommandService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class MutationGraphQlController(
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
    private val reservationCommandService: ReservationCommandService,
    private val wishCommandService: WishCommandService,
    private val productReviewCommandService: ProductReviewCommandService,
    private val productReviewQueryService: ProductReviewQueryService,
) {

    @MutationMapping
    fun createReservation(
        @Argument productId: String,
        @Argument input: CreateReservationGraphQlInput,
    ): ReservationCreatedGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val created = reservationCommandService.createReservation(
            CreateReservationCommand(
                productId = productId.toLongOrNull()
                    ?: throw IllegalArgumentException("productId must be numeric."),
                reservationSlotId = input.reservationSlotId.toLongOrNull()
                    ?: throw IllegalArgumentException("reservationSlotId must be numeric."),
                userId = userId,
                totalPrice = input.totalPrice.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("totalPrice must be numeric.")
            )
        )
        return ReservationCreatedGraphQlResponse.from(created)
    }

    @MutationMapping
    fun cancelReservation(@Argument reservationId: String): ReservationSummaryGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val reservation = reservationCommandService.cancelReservationByUser(
            reservationId = reservationId.toLongOrNull()
                ?: throw IllegalArgumentException("reservationId must be numeric."),
            userId = userId
        )
        return ReservationSummaryGraphQlResponse.from(
            ReservationSummaryResponse.from(
                reservation,
                reservation.resolveStatus()
            )
        )
    }

    @MutationMapping
    fun toggleWish(@Argument productId: String): WishToggleGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val response = wishCommandService.toggle(
            userId = userId,
            productId = productId.toLongOrNull()
                ?: throw IllegalArgumentException("productId must be numeric.")
        )
        return WishToggleGraphQlResponse.from(response)
    }

    @MutationMapping
    fun createProductReview(
        @Argument productId: String,
        @Argument input: CreateProductReviewGraphQlInput,
    ): ProductReviewGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val review = productReviewCommandService.createReview(
            CreateProductReviewCommand(
                productId = productId.toLongOrNull()
                    ?: throw IllegalArgumentException("productId must be numeric."),
                userId = userId,
                rating = input.rating,
                content = input.content
            )
        )
        return ProductReviewGraphQlResponse.from(productReviewQueryService.toResponse(review))
    }
}
