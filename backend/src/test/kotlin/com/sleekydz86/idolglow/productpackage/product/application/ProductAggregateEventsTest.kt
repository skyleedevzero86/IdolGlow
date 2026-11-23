package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.admin.application.AdminProductReviewService
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductAggregateCounter
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.review.application.ProductReviewCommandService
import com.sleekydz86.idolglow.review.application.ProductReviewTrustCommandService
import com.sleekydz86.idolglow.review.application.dto.UpdateProductReviewCommand
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.wish.application.WishCommandService
import com.sleekydz86.idolglow.wish.application.WishEventListener
import com.sleekydz86.idolglow.wish.application.event.WishDeleteEvent
import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("상품 집계 칸: 찜·리뷰 사건마다 실제 행 수와 같게 바뀐다")
class ProductAggregateEventsTest {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var productReviewJpaRepository: ProductReviewJpaRepository

    @Autowired private lateinit var productAggregateCounter: ProductAggregateCounter

    @Autowired private lateinit var wishCommandService: WishCommandService

    @Autowired private lateinit var wishEventListener: WishEventListener

    @Autowired private lateinit var productReviewCommandService: ProductReviewCommandService

    @Autowired private lateinit var productReviewTrustCommandService: ProductReviewTrustCommandService

    @Autowired private lateinit var adminProductReviewService: AdminProductReviewService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    private val token = UUID.randomUUID().toString().take(8)

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @DisplayName("찜을 추가하면 찜 수가 1 늘고, 다시 누르면 1 준다 (E1, E2)")
    @Test
    fun wishToggleChangesWishCount() {
        // given
        val product = product()
        val user = user(1)

        // when
        wishCommandService.toggle(user.id, product.id)
        val afterAdd = aggregates(product.id)
        wishCommandService.toggle(user.id, product.id)
        val afterRemove = aggregates(product.id)

        // then
        assertEquals(listOf(Triple(1L, 0L, 0L), Triple(0L, 0L, 0L)), listOf(afterAdd, afterRemove))
        assertEquals(recount(product.id), aggregates(product.id))
    }

    @DisplayName("공개 리뷰의 별점을 고치면 합계만 차이만큼 바뀌고, 숨긴 리뷰의 별점을 고치면 그대로다 (E5, E6)")
    @Test
    fun ratingChangeAppliesOnlyToVisibleReview() {
        // given
        val product = product()
        val visible = review(product, user(1), rating = 5, hidden = false)
        val hidden = review(product, user(2), rating = 1, hidden = true)

        // when
        productReviewCommandService.updateReview(UpdateProductReviewCommand(product.id, visible.id, visible.userId, 2, "고친 리뷰"))
        val afterVisible = aggregates(product.id)
        productReviewCommandService.updateReview(UpdateProductReviewCommand(product.id, hidden.id, hidden.userId, 4, "고친 리뷰"))
        val afterHidden = aggregates(product.id)

        // then
        assertEquals(listOf(Triple(0L, 1L, 2L), Triple(0L, 1L, 2L)), listOf(afterVisible, afterHidden))
        assertEquals(recount(product.id), aggregates(product.id))
    }

    @DisplayName("숨긴 리뷰를 지우면 그대로이고, 공개 리뷰를 지우면 수와 합계가 그만큼 준다 (E7, E8)")
    @Test
    fun deleteAppliesOnlyToVisibleReview() {
        // given
        val product = product()
        val visible = review(product, user(1), rating = 5, hidden = false)
        val hidden = review(product, user(2), rating = 1, hidden = true)

        // when
        productReviewCommandService.deleteReview(product.id, hidden.id, hidden.userId)
        val afterHidden = aggregates(product.id)
        productReviewCommandService.deleteReview(product.id, visible.id, visible.userId)
        val afterVisible = aggregates(product.id)

        // then
        assertEquals(listOf(Triple(0L, 1L, 5L), Triple(0L, 0L, 0L)), listOf(afterHidden, afterVisible))
        assertEquals(recount(product.id), aggregates(product.id))
    }

    @DisplayName("관리자 숨김·숨김 해제는 공개 상태가 실제로 바뀔 때만 수와 합계를 바꾼다 (E9~E12)")
    @Test
    fun adminHideAndUnhideApplyOnlyOnVisibilityChange() {
        // given
        val product = product()
        val review = review(product, user(1), rating = 4, hidden = false)
        loginAsAdmin(user(9))

        // when
        adminProductReviewService.hideReview(review.id, "관리자 숨김")
        val afterHide = aggregates(product.id)
        adminProductReviewService.hideReview(review.id, "다시 숨김")
        val afterHideAgain = aggregates(product.id)
        adminProductReviewService.unhideReview(review.id)
        val afterUnhide = aggregates(product.id)
        adminProductReviewService.unhideReview(review.id)
        val afterUnhideAgain = aggregates(product.id)

        // then
        assertEquals(
            listOf(Triple(0L, 0L, 0L), Triple(0L, 0L, 0L), Triple(0L, 1L, 4L), Triple(0L, 1L, 4L)),
            listOf(afterHide, afterHideAgain, afterUnhide, afterUnhideAgain),
        )
        assertEquals(recount(product.id), aggregates(product.id))
    }

    @DisplayName("신고가 3건 쌓여 자동으로 숨겨지면 수와 합계가 그만큼 준다 (E9)")
    @Test
    fun autoHideByReportsDecreasesAggregate() {
        // given
        val product = product()
        val review = review(product, user(1), rating = 3, hidden = false)
        val reporters = (2..4).map { user(it) }

        // when
        reporters.forEach { productReviewTrustCommandService.reportReview(product.id, review.id, it.id, "부적절한 내용") }

        // then
        assertEquals(Triple(0L, 0L, 0L), aggregates(product.id))
        assertEquals(recount(product.id), aggregates(product.id))
    }

    @DisplayName("회원 탈퇴로 그 회원의 찜이 지워지면 찜한 상품마다 찜 수가 줄고, 다른 회원의 찜은 남는다 (E3)")
    @Test
    fun userWithdrawalDecreasesWishCountsPerProduct() {
        // given
        val first = product()
        val second = product()
        val leaving = user(1)
        val staying = user(2)
        wishCommandService.toggle(leaving.id, first.id)
        wishCommandService.toggle(leaving.id, second.id)
        wishCommandService.toggle(staying.id, first.id)

        // when
        wishEventListener.handleWishDelete(WishDeleteEvent(WishAggregateType.USER, leaving.id))

        // then
        assertEquals(listOf(Triple(1L, 0L, 0L), Triple(0L, 0L, 0L)), listOf(aggregates(first.id), aggregates(second.id)))
        assertEquals(listOf(recount(first.id), recount(second.id)), listOf(aggregates(first.id), aggregates(second.id)))
    }

    @DisplayName("상품을 읽어 둔 사이 다른 트랜잭션이 찜해도, 그 상품을 고쳐 저장할 때 찜 수가 덮어써지지 않는다")
    @Test
    fun productUpdateDoesNotOverwriteWishCount() {
        // given
        val product = product()
        val user = user(1)
        val requiresNew =
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }

        // when
        transactionTemplate.executeWithoutResult {
            val loaded = productJpaRepository.findById(product.id).orElseThrow()
            requiresNew.executeWithoutResult { wishCommandService.toggle(user.id, product.id) }
            loaded.name = "고친 상품 $token"
        }

        // then
        assertEquals(Triple(1L, 0L, 0L), aggregates(product.id))
        assertEquals("고친 상품 $token", productJpaRepository.findById(product.id).orElseThrow().name)
    }

    private fun product(): Product = productJpaRepository.saveAndFlush(Product(name = "집계 $token", description = "집계 사건 시험"))

    private fun user(index: Int): User =
        userJpaRepository.findByEmail("agg$token$index@idolglow.test")
            ?: userJpaRepository.save(User.of(email = "agg$token$index@idolglow.test"))

    private fun review(
        product: Product,
        user: User,
        rating: Int,
        hidden: Boolean,
    ): ProductReview =
        checkNotNull(
            transactionTemplate.execute {
                val review = ProductReview.of(product = product, userId = user.id, ratingScore = rating, content = "별점 $rating")
                if (hidden) review.hide(LocalDateTime.now(), "시험 숨김")
                val saved = productReviewJpaRepository.save(review)
                if (!hidden) productAggregateCounter.changeReviewAggregate(product.id, 1L, rating.toLong())
                saved
            },
        )

    private fun loginAsAdmin(admin: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(admin.id.toString(), "", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    }

    private fun aggregates(productId: Long): Triple<Long, Long, Long> =
        jdbcTemplate.queryForObject(
            "select wish_count, review_count, rating_sum from products where id = ?",
            { rs, _ -> Triple(rs.getLong(1), rs.getLong(2), rs.getLong(3)) },
            productId,
        )!!

    private fun recount(productId: Long): Triple<Long, Long, Long> =
        jdbcTemplate.queryForObject(
            "select (select count(*) from wishes where product_id = ?), " +
                "(select count(*) from product_reviews where product_id = ? and hidden_at is null), " +
                "(select coalesce(sum(rating), 0) from product_reviews where product_id = ? and hidden_at is null)",
            { rs, _ -> Triple(rs.getLong(1), rs.getLong(2), rs.getLong(3)) },
            productId,
            productId,
            productId,
        )!!
}
