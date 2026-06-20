package com.sleekydz86.idolglow.review.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.review.adapter.web.request.UpdateProductReviewRequest
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import com.sleekydz86.idolglow.review.application.ProductReviewTrustCommandService
import com.sleekydz86.idolglow.review.application.dto.CreateProductReviewCommand
import com.sleekydz86.idolglow.review.application.dto.UpdateProductReviewCommand
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class ProductReviewMutationGraphQlController(
    private val productReviewCommandService: ProductReviewCommandService,
    private val productReviewQueryService: ProductReviewQueryService,
    private val productReviewTrustCommandService: ProductReviewTrustCommandService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @MutationMapping
    fun createProductReview(
        @Argument productId: String,
        @Argument input: CreateProductReviewGraphQlInput,
    ): ProductReviewGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val review =
            productReviewCommandService.createReview(
                CreateProductReviewCommand(
                    productId = productId.toGraphQlIdLong("productId"),
                    userId = userId,
                    reservationId = input.reservationId.toGraphQlIdLong("reservationId"),
                    rating = input.rating,
                    content = input.content,
                ),
            )
        return ProductReviewGraphQlResponse.from(productReviewQueryService.toResponse(review))
    }

    @MutationMapping
    fun updateProductReview(
        @Argument productId: String,
        @Argument reviewId: String,
        @Argument @Valid input: UpdateProductReviewRequest,
    ): ProductReviewGraphQlResponse {
        val review =
            productReviewCommandService.updateReview(
                UpdateProductReviewCommand(
                    productId = productId.toGraphQlIdLong("productId"),
                    reviewId = reviewId.toGraphQlIdLong("reviewId"),
                    userId = authenticatedUserIdResolver.resolveRequired(),
                    rating = input.rating,
                    content = input.content,
                ),
            )
        return ProductReviewGraphQlResponse.from(productReviewQueryService.toResponse(review))
    }

    @MutationMapping
    fun deleteProductReview(
        @Argument productId: String,
        @Argument reviewId: String,
    ): Boolean {
        productReviewCommandService.deleteReview(
            productId = productId.toGraphQlIdLong("productId"),
            reviewId = reviewId.toGraphQlIdLong("reviewId"),
            userId = authenticatedUserIdResolver.resolveRequired(),
        )
        return true
    }

    @MutationMapping
    fun toggleProductReviewHelpful(
        @Argument productId: String,
        @Argument reviewId: String,
    ): Int {
        val count =
            productReviewTrustCommandService.toggleHelpful(
                productId = productId.toGraphQlIdLong("productId"),
                reviewId = reviewId.toGraphQlIdLong("reviewId"),
                userId = authenticatedUserIdResolver.resolveRequired(),
            )
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    @MutationMapping
    fun reportProductReview(
        @Argument productId: String,
        @Argument reviewId: String,
        @Argument reason: String,
    ): Boolean {
        productReviewTrustCommandService.reportReview(
            productId = productId.toGraphQlIdLong("productId"),
            reviewId = reviewId.toGraphQlIdLong("reviewId"),
            reporterUserId = authenticatedUserIdResolver.resolveRequired(),
            reason = reason,
        )
        return true
    }
}
