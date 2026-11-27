package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.infrastructure.WishJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("상품 집계 정렬 (PostgreSQL)")
class ProductAggregateSortPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var wishJpaRepository: WishJpaRepository

    @Autowired private lateinit var productReviewJpaRepository: ProductReviewJpaRepository

    @Autowired private lateinit var productAggregateCounter: ProductAggregateCounter

    @Autowired private lateinit var productSearchQueryRepository: ProductSearchQueryRepository

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @DisplayName("인기순·별점순·리뷰 많은 순이 PostgreSQL에서 오류 없이 찜 수·평균 별점·공개 리뷰 수·ID 순서로 정렬된다")
    @Test
    fun aggregateSortsRunOnPostgres() {
        // given
        val scenario =
            ProductAggregateSortScenario(
                productJpaRepository,
                userJpaRepository,
                wishJpaRepository,
                productReviewJpaRepository,
                productAggregateCounter,
            )
        val ids = checkNotNull(transactionTemplate.execute { scenario.seed() })

        // when
        val orders =
            listOf(ProductSort.POPULARITY, ProductSort.RATING, ProductSort.REVIEW_COUNT).map { sort ->
                productSearchQueryRepository.findOrderedProductIds(scenario.criteria(sort))
            }

        // then
        assertEquals(
            listOf(
                ProductAggregateSortScenario.POPULARITY_ORDER,
                ProductAggregateSortScenario.RATING_ORDER,
                ProductAggregateSortScenario.REVIEW_COUNT_ORDER,
            ).map { labels -> labels.map(ids::getValue) },
            orders,
        )
    }
}
