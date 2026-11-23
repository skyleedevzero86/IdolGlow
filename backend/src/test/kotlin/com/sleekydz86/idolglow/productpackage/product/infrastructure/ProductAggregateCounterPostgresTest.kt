package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.application.WishCommandService
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

@DisplayName("상품 집계 칸 증감 (PostgreSQL)")
class ProductAggregateCounterPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var wishCommandService: WishCommandService

    @Autowired private lateinit var productAggregateCounter: ProductAggregateCounter

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    private val token = UUID.randomUUID().toString().take(8)

    @DisplayName("회원 20명이 같은 상품을 동시에 찜해도 찜 수가 찜 행 수 20과 같다")
    @Test
    fun concurrentWishesKeepCountExact() {
        // given
        val product = productJpaRepository.saveAndFlush(Product(name = "동시 찜 $token", description = "동시성 시험"))
        val users = (1..USERS).map { userJpaRepository.save(User.of(email = "race$token$it@idolglow.test")) }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(USERS)

        // when
        val futures =
            users.map { user ->
                executor.submit {
                    start.await()
                    wishCommandService.toggle(user.id, product.id)
                }
            }
        start.countDown()
        futures.forEach { it.get(WAIT_SECONDS, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        val wishRows = jdbcTemplate.queryForObject("select count(*) from wishes where product_id = ?", Long::class.java, product.id)
        assertEquals(USERS.toLong(), wishRows)
        assertEquals(USERS.toLong(), wishCount(product.id))
    }

    @DisplayName("찜 수를 0 밑으로 내리는 감소는 실패하고 찜 수는 그대로 0이다")
    @Test
    fun decreaseBelowZeroFails() {
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
        private const val WAIT_SECONDS: Long = 30
    }
}
