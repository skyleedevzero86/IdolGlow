package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.infrastructure.WishJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("상품 집계 정렬 순서 고정 (H2)")
class ProductAggregateSortTest {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var wishJpaRepository: WishJpaRepository

    @Autowired private lateinit var productReviewJpaRepository: ProductReviewJpaRepository

    @Autowired private lateinit var productAggregateCounter: ProductAggregateCounter

    @Autowired private lateinit var productSearchQueryRepository: ProductSearchQueryRepository

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @DisplayName("찜 동점·평균 별점 동점·숨긴 리뷰·리뷰 없는 상품이 섞여도 세 집계 정렬의 상품 순서가 정해진 대로 나온다")
    @Test
    fun aggregateSortOrderIsStable() {
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
        val popularity = productSearchQueryRepository.findOrderedProductIds(scenario.criteria(ProductSort.POPULARITY))
        val rating = productSearchQueryRepository.findOrderedProductIds(scenario.criteria(ProductSort.RATING))
        val reviewCount = productSearchQueryRepository.findOrderedProductIds(scenario.criteria(ProductSort.REVIEW_COUNT))

        // then
        assertEquals(ProductAggregateSortScenario.POPULARITY_ORDER.map(ids::getValue), popularity)
        assertEquals(ProductAggregateSortScenario.RATING_ORDER.map(ids::getValue), rating)
        assertEquals(ProductAggregateSortScenario.REVIEW_COUNT_ORDER.map(ids::getValue), reviewCount)
    }
}
