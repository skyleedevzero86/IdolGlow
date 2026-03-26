package com.sleekydz86.idolglow.review.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductReviewGraphQlController(
    private val productReviewQueryService: ProductReviewQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun productReviews(@Argument productId: String): List<ProductReviewGraphQlResponse> {
        val parsedProductId = productId.toLongOrNull()
            ?: throw IllegalArgumentException("productId must be numeric.")

        return productReviewQueryService.findReviewsByProduct(parsedProductId)
            .map(ProductReviewGraphQlResponse::from)
    }

    @QueryMapping
    fun myReviews(): List<ProductReviewGraphQlResponse> =
        productReviewQueryService.findReviewsByUser(authenticatedUserIdResolver.resolveRequired())
            .map(ProductReviewGraphQlResponse::from)
}

data class ProductReviewGraphQlResponse(
    val reviewId: String,
    val productId: String,
    val userId: String,
    val rating: Int,
    val content: String,
    val createdAt: String,
    val images: List<ProductReviewImageGraphQlResponse>,
) {
    companion object {
        fun from(response: ProductReviewResponse): ProductReviewGraphQlResponse =
            ProductReviewGraphQlResponse(
                reviewId = response.reviewId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                userId = response.userId.asGraphQlId(),
                rating = response.rating,
                content = response.content,
                createdAt = response.createdAt.toString(),
                images = response.images.map(ProductReviewImageGraphQlResponse::from)
            )
    }
}

data class ProductReviewImageGraphQlResponse(
    val id: String,
    val originalFilename: String,
    val url: String,
    val sortOrder: Int,
) {
    companion object {
        fun from(response: ProductReviewImageResponse): ProductReviewImageGraphQlResponse =
            ProductReviewImageGraphQlResponse(
                id = response.id.asGraphQlId(),
                originalFilename = response.originalFilename,
                url = response.url,
                sortOrder = response.sortOrder
            )
    }
}
