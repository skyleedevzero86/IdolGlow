package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.application.WishCommandService
import com.sleekydz86.idolglow.wish.infrastructure.WishJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("상품 집계 칸과 집계 정렬 (MySQL)")
class ProductAggregateMysqlTest : MysqlIntegrationTest() {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var wishJpaRepository: WishJpaRepository

    @Autowired private lateinit var productReviewJpaRepository: ProductReviewJpaRepository

    @Autowired private lateinit var productAggregateCounter: ProductAggregateCounter

    @Autowired private lateinit var productSearchQueryRepository: ProductSearchQueryRepository

    @Autowired private lateinit var wishCommandService: WishCommandService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    private val token = UUID.randomUUID().toString().take(8)

    @DisplayName("인기순·별점순·리뷰 많은 순이 MySQL에서 PostgreSQL과 같은 순서로 정렬된다")
    @Test
    fun aggregateSortsRunOnMysql() {
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

    @DisplayName("회원 20명이 같은 상품을 동시에 찜해도 데드락 없이 모두 성공하고 찜 수가 찜 행 수 20과 같다")
    @Test
    fun concurrentWishesSucceedWithoutDeadlock() {
        // given
        val product = productJpaRepository.saveAndFlush(Product(name = "동시 찜 $token", description = "동시성 시험"))
        val users =
            (1..USERS).map {
                userJpaRepository.save(User.of(email = "my$token$it@idolglow.test", nickname = "m${token.take(6)}$it"))
            }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(USERS)

        // when
        val futures =
            users.map { user ->
                executor.submit<Result<Unit>> {
                    start.await()
                    runCatching { wishCommandService.toggle(user.id, product.id) }.map { }
                }
            }
        start.countDown()
        val failures = futures.map { it.get(WAIT_SECONDS, TimeUnit.SECONDS) }.mapNotNull { it.exceptionOrNull() }
        executor.shutdown()

        // then
        assertEquals(emptyList<String>(), failures.map { "${it.javaClass.simpleName}: ${it.message}" })
        val wishRows =
            jdbcTemplate.queryForObject(WISH_ROWS_SQL, Long::class.java, product.id)
        assertEquals(USERS.toLong(), wishRows)
        assertEquals(USERS.toLong(), wishCount(product.id))
    }

    @DisplayName("찜 수를 0 밑으로 내리는 감소는 MySQL CHECK 제약으로 실패하고 찜 수는 그대로 0이다")
    @Test
    fun decreaseBelowZeroFailsOnMysql() {
        // given
        val product = productJpaRepository.saveAndFlush(Product(name = "음수 $token", description = "제약 시험"))

        // when
        val failure =
            assertThrows<DataIntegrityViolationException> {
                transactionTemplate.executeWithoutResult { productAggregateCounter.changeWishCount(product.id, -1L) }
            }

        // then
        assertEquals(true, failure.message.orEmpty().contains("chk_products_aggregate_non_negative"))
        assertEquals(0L, wishCount(product.id))
    }

    private fun wishCount(productId: Long): Long =
        jdbcTemplate.queryForObject("select wish_count from products where id = ?", Long::class.java, productId)!!

    companion object {
        private const val USERS: Int = 20
        private const val WAIT_SECONDS: Long = 60
        private const val WISH_ROWS_SQL: String = "select count(*) from wishes where product_id = ?"
    }
}
