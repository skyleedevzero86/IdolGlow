package com.sleekydz86.idolglow.review.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ProductReviewGraphQlController(
    private val productReviewQueryService: ProductReviewQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun productReviews(@Argument productId: String): List<ProductReviewGraphQlResponse> {
        val parsedProductId = productId.toLongOrNull()
            ?: throw IllegalArgumentException("productId는 숫자여야 합니다.")

        return productReviewQueryService.findReviewsByProduct(parsedProductId)
            .map(ProductReviewGraphQlResponse::from)
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myReviews(): List<ProductReviewGraphQlResponse> =
        productReviewQueryService.findReviewsByUser(authenticatedUserIdResolver.resolveRequired())
            .map(ProductReviewGraphQlResponse::from)
}
