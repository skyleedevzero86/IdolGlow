package com.sleekydz86.idolglow.review.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import com.sleekydz86.idolglow.review.application.dto.UpdateProductReviewCommand
import com.sleekydz86.idolglow.review.ui.request.UpdateProductReviewRequest
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
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @MutationMapping
    fun updateProductReview(
        @Argument productId: String,
        @Argument reviewId: String,
        @Argument @Valid input: UpdateProductReviewRequest,
    ): ProductReviewGraphQlResponse {
        val review = productReviewCommandService.updateReview(
            UpdateProductReviewCommand(
                productId = productId.toGraphQlIdLong("productId"),
                reviewId = reviewId.toGraphQlIdLong("reviewId"),
                userId = authenticatedUserIdResolver.resolveRequired(),
                rating = input.rating,
                content = input.content
            )
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
            userId = authenticatedUserIdResolver.resolveRequired()
        )
        return true
    }
}
