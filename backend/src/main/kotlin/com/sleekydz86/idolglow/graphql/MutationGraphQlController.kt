package com.sleekydz86.idolglow.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.graphql.SlotWaitlistEntryGraphQlResponse
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.graphql.CreateReservationGraphQlInput
import com.sleekydz86.idolglow.graphql.ReservationCreatedGraphQlResponse
import com.sleekydz86.idolglow.graphql.WishToggleGraphQlResponse
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.graphql.ReservationSummaryGraphQlResponse
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import com.sleekydz86.idolglow.review.application.dto.CreateProductReviewCommand
import com.sleekydz86.idolglow.graphql.CreateProductReviewGraphQlInput
import com.sleekydz86.idolglow.review.graphql.ProductReviewGraphQlResponse
import com.sleekydz86.idolglow.wish.application.WishCommandService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class MutationGraphQlController(
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
    private val reservationCommandService: ReservationCommandService,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
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
                    ?: throw IllegalArgumentException("productId는 숫자여야 합니다."),
                reservationSlotId = input.reservationSlotId.toLongOrNull()
                    ?: throw IllegalArgumentException("reservationSlotId는 숫자여야 합니다."),
                userId = userId,
                totalPrice = input.totalPrice.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("totalPrice는 숫자여야 합니다.")
            )
        )
        return ReservationCreatedGraphQlResponse.from(created)
    }

    @MutationMapping
    fun registerSlotWaitlist(
        @Argument productId: String,
        @Argument reservationSlotId: String,
    ): SlotWaitlistEntryGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val pid = productId.toLongOrNull() ?: throw IllegalArgumentException("productId는 숫자여야 합니다.")
        val sid = reservationSlotId.toLongOrNull()
            ?: throw IllegalArgumentException("reservationSlotId는 숫자여야 합니다.")
        val entry = reservationSlotWaitlistService.register(userId, pid, sid)
        return SlotWaitlistEntryGraphQlResponse.from(entry)
    }

    @MutationMapping
    fun unregisterSlotWaitlist(
        @Argument productId: String,
        @Argument reservationSlotId: String,
    ): Boolean {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val pid = productId.toLongOrNull() ?: throw IllegalArgumentException("productId는 숫자여야 합니다.")
        val sid = reservationSlotId.toLongOrNull()
            ?: throw IllegalArgumentException("reservationSlotId는 숫자여야 합니다.")
        reservationSlotWaitlistService.unregister(userId, pid, sid)
        return true
    }

    @MutationMapping
    fun cancelReservation(@Argument reservationId: String): ReservationSummaryGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val reservation = reservationCommandService.cancelReservationByUser(
            reservationId = reservationId.toLongOrNull()
                ?: throw IllegalArgumentException("reservationId는 숫자여야 합니다."),
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
                ?: throw IllegalArgumentException("productId는 숫자여야 합니다.")
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
                    ?: throw IllegalArgumentException("productId는 숫자여야 합니다."),
                userId = userId,
                reservationId = input.reservationId.toLongOrNull()
                    ?: throw IllegalArgumentException("reservationId는 숫자여야 합니다."),
                rating = input.rating,
                content = input.content
            )
        )
        return ProductReviewGraphQlResponse.from(productReviewQueryService.toResponse(review))
    }
}
