package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSearchCriteria
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.domain.Wish
import com.sleekydz86.idolglow.wish.infrastructure.WishJpaRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ProductAggregateSortScenario(
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val wishJpaRepository: WishJpaRepository,
    private val productReviewJpaRepository: ProductReviewJpaRepository,
    private val productAggregateCounter: ProductAggregateCounter,
) {
    val keyword: String = "aggsort${UUID.randomUUID().toString().take(8)}"

    fun seed(): Map<String, Long> {
        val users = (1..3).map { userJpaRepository.save(User.of(email = "$keyword$it@idolglow.test")) }
        val products =
            PRODUCT_SPECS.associate { spec ->
                val product = productJpaRepository.saveAndFlush(Product(name = "$keyword ${spec.label}", description = "집계 정렬 시험"))
                users.take(spec.wishes).forEach { wishJpaRepository.save(Wish(user = it, product = product)) }
                spec.visibleRatings.forEachIndexed { index, rating -> review(product, users[index], rating, hidden = false) }
                spec.hiddenRatings.forEachIndexed { index, rating -> review(product, users[2 - index], rating, hidden = true) }
                productAggregateCounter.changeWishCount(product.id, spec.wishes.toLong())
                productAggregateCounter.changeReviewAggregate(
                    product.id,
                    spec.visibleRatings.size.toLong(),
                    spec.visibleRatings.sum().toLong(),
                )
                spec.label to product.id
            }
        wishJpaRepository.flush()
        productReviewJpaRepository.flush()
        return products
    }

    fun criteria(sort: ProductSort): ProductSearchCriteria =
        ProductSearchCriteria(
            lastId = null,
            offset = 0,
            size = 20,
            tag = null,
            tags = emptyList(),
            keyword = keyword,
            minPrice = null,
            maxPrice = null,
            visitDate = null,
            reservableOnly = false,
            sort = sort,
            nearLatitude = null,
            nearLongitude = null,
            radiusMeters = null,
            now = LocalDateTime.now(),
            today = LocalDate.now(),
        )

    private fun review(
        product: Product,
        user: User,
        rating: Int,
        hidden: Boolean,
    ) {
        val review = ProductReview.of(product = product, userId = user.id, ratingScore = rating, content = "별점 $rating")
        if (hidden) review.hide(LocalDateTime.now(), "시험 숨김")
        productReviewJpaRepository.save(review)
    }

    private data class ProductSpec(
        val label: String,
        val wishes: Int,
        val visibleRatings: List<Int>,
        val hiddenRatings: List<Int> = emptyList(),
    )

    companion object {
        val POPULARITY_ORDER = listOf("F", "B", "A", "C", "D", "E")
        val RATING_ORDER = listOf("C", "D", "F", "B", "A", "E")
        val REVIEW_COUNT_ORDER = listOf("D", "F", "B", "A", "C", "E")

        private val PRODUCT_SPECS =
            listOf(
                ProductSpec("A", wishes = 2, visibleRatings = listOf(5, 3)),
                ProductSpec("B", wishes = 2, visibleRatings = listOf(4, 4)),
                ProductSpec("C", wishes = 1, visibleRatings = listOf(5), hiddenRatings = listOf(1)),
                ProductSpec("D", wishes = 0, visibleRatings = listOf(5, 5, 4)),
                ProductSpec("E", wishes = 0, visibleRatings = emptyList()),
                ProductSpec("F", wishes = 2, visibleRatings = listOf(5, 4, 3)),
            )
    }
}
