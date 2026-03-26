package com.sleekydz86.idolglow.review.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewQueryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/products/{productId}/reviews")
class ProductReviewController(
    private val productReviewCommandService: ProductReviewCommandService,
    private val productReviewQueryService: ProductReviewQueryService
) : ProductReviewApi {

    @GetMapping
    override fun findReviews(@PathVariable productId: Long): List<ProductReviewResponse> =
        productReviewQueryService.findReviewsByProduct(productId)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    override fun createReview(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestPart("request") request: CreateProductReviewRequest,
        @RequestPart("images", required = false) images: List<MultipartFile>?
    ): ProductReviewResponse {
        val command = CreateProductReviewCommand(
            productId = productId,
            userId = userId,
            rating = request.rating,
            content = request.content
        )
        val imageFiles = toReviewImageFiles(images)
        val review = productReviewCommandService.createReview(command, imageFiles)
        return productReviewQueryService.toResponse(review)
    }

    @PutMapping("/{reviewId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun updateReview(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @PathVariable reviewId: Long,
        @Valid @RequestPart("request") request: UpdateProductReviewRequest,
        @RequestPart("images", required = false) images: List<MultipartFile>?
    ): ProductReviewResponse {
        val command = UpdateProductReviewCommand(
            productId = productId,
            reviewId = reviewId,
            userId = userId,
            rating = request.rating,
            content = request.content
        )
        val imageFiles = images?.let { toReviewImageFiles(it) }
        val review = productReviewCommandService.updateReview(command, imageFiles)
        return productReviewQueryService.toResponse(review)
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteReview(
        @LoginUser userId: Long,
        @PathVariable productId: Long,
        @PathVariable reviewId: Long
    ) {
        productReviewCommandService.deleteReview(productId, reviewId, userId)
    }

    private fun toReviewImageFiles(images: List<MultipartFile>?): List<ReviewImageFile> =
        images.orEmpty()
            .filterNot { it.isEmpty }
            .mapIndexed { index, file ->
                ReviewImageFile(
                    originalFilename = file.originalFilename ?: "review-image-$index",
                    content = file.bytes,
                    sortOrder = index
                )
            }
}
