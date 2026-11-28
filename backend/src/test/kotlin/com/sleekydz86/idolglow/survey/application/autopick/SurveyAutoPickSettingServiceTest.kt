package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.AutoPickExceptionType
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.QuickBookExceptionType
import com.sleekydz86.idolglow.payment.application.MembershipQuery
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickProduct
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickSetting
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickProductJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickSettingJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("자동 담기 설정")
class SurveyAutoPickSettingServiceTest {
    private val settingRepository = mockk<SurveyAutoPickSettingJpaRepository>()
    private val productRepository = mockk<SurveyAutoPickProductJpaRepository>(relaxed = true)
    private val candidateRepository = mockk<SurveyAutoPickCandidateJpaRepository>()
    private val catalog = mockk<ProductCommandRepository>()
    private val membershipQuery = mockk<MembershipQuery>()
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val counter = mockk<SurveyAutoPickMonthlyCounter>()
    private val service =
        SurveyAutoPickSettingService(
            settingRepository,
            productRepository,
            candidateRepository,
            catalog,
            membershipQuery,
            methodRepository,
            counter,
        )
    private val now = LocalDateTime.of(2026, 11, 29, 12, 0)

    @BeforeEach
    fun setUp() {
        every { settingRepository.findByUserId(USER_ID) } returns null
        every { settingRepository.save(any()) } answers { firstArg() }
        every { productRepository.findAllBySettingId(any()) } returns emptyList()
        every { candidateRepository.findTop20ByUserIdOrderByScheduledChargeAtDesc(USER_ID) } returns emptyList()
        every { catalog.existsById(any()) } returns true
        every { membershipQuery.isActive(USER_ID, now) } returns true
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns
            TossBillingMethod(userId = USER_ID, customerKey = "c", billingKey = "bk")
        every { counter.bookedInMonth(USER_ID, now) } returns 0L
    }

    @DisplayName("A2 상품이 0개거나 11개면 400 AUTO_PICK_INVALID_PRODUCTS 이다")
    @ParameterizedTest(name = "상품 {0}개")
    @ValueSource(ints = [0, 11])
    fun productCountOutsideRangeRejected(count: Int) {
        // when
        val error = errorOf { service.update(USER_ID, command(productCount = count), now) }

        // then
        assertThat(error).isEqualTo(AutoPickExceptionType.INVALID_PRODUCTS)
        assertThat(error?.httpStatusCode).isEqualTo(400)
        verify(exactly = 0) { settingRepository.save(any()) }
    }

    @DisplayName("A2 상품이 1개와 10개면 저장한다")
    @ParameterizedTest(name = "상품 {0}개")
    @ValueSource(ints = [1, 10])
    fun productCountBoundaryAccepted(count: Int) {
        // when
        val error = errorOf { service.update(USER_ID, command(productCount = count), now) }

        // then
        assertThat(error).isNull()
        verify { productRepository.saveAll(match<List<SurveyAutoPickProduct>> { it.size == count }) }
    }

    @DisplayName("A2 예산 상한이 -1 이면 400 AUTO_PICK_INVALID_MAX_PRICE 이고 0 이면 저장한다")
    @Test
    fun maxPriceBoundary() {
        // when
        val negative = errorOf { service.update(USER_ID, command(maxPrice = "-1"), now) }
        val zero = errorOf { service.update(USER_ID, command(maxPrice = "0"), now) }

        // then
        assertThat(negative).isEqualTo(AutoPickExceptionType.INVALID_MAX_PRICE)
        assertThat(zero).isNull()
    }

    @DisplayName("A2 없는 상품이 있으면 404 AUTO_PICK_PRODUCT_NOT_FOUND 이다")
    @Test
    fun unknownProductRejected() {
        // given
        every { catalog.existsById(3L) } returns false

        // when
        val error = errorOf { service.update(USER_ID, command(productCount = 3), now) }

        // then
        assertThat(error).isEqualTo(AutoPickExceptionType.PRODUCT_NOT_FOUND)
    }

    @DisplayName("A3 Glow Pass 회원이 아니면 켤 때 403 AUTO_PICK_MEMBERSHIP_REQUIRED 이다")
    @Test
    fun enableRequiresMembership() {
        // given
        every { membershipQuery.isActive(USER_ID, now) } returns false

        // when
        val error = errorOf { service.update(USER_ID, command(), now) }

        // then
        assertThat(error).isEqualTo(AutoPickExceptionType.MEMBERSHIP_REQUIRED)
        assertThat(error?.httpStatusCode).isEqualTo(403)
    }

    @DisplayName("A3 기본 카드가 없으면 켤 때 400 BILLING_METHOD_REQUIRED 이다")
    @Test
    fun enableRequiresCard() {
        // given
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns null

        // when
        val error = errorOf { service.update(USER_ID, command(), now) }

        // then
        assertThat(error).isEqualTo(QuickBookExceptionType.BILLING_METHOD_REQUIRED)
    }

    @DisplayName("A3 끄는 저장은 회원·카드 조건을 보지 않는다")
    @Test
    fun disableSkipsReadiness() {
        // given
        every { membershipQuery.isActive(USER_ID, now) } returns false
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns null

        // when
        val error = errorOf { service.update(USER_ID, command(enabled = false), now) }

        // then
        assertThat(error).isNull()
    }

    @DisplayName("A6 다른 회원의 후보는 404 AUTO_PICK_CANDIDATE_NOT_FOUND 이고 상태가 그대로다")
    @Test
    fun skipOtherUsersCandidateRejected() {
        // given
        val candidate = candidate(userId = 99L)
        every { candidateRepository.findByIdForUpdate(5L) } returns candidate

        // when
        val error = errorOf { service.skip(USER_ID, 5L, now) }

        // then
        assertThat(error).isEqualTo(AutoPickExceptionType.CANDIDATE_NOT_FOUND)
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.SCHEDULED)
    }

    @DisplayName("A6 결제 예정 시각이 되면 409 AUTO_PICK_SKIP_CLOSED 이고, 1초 전이면 건너뛴다")
    @Test
    fun skipClosesAtChargeTime() {
        // given
        val late = candidate()
        val early = candidate()
        every { candidateRepository.findByIdForUpdate(5L) } returns late
        every { candidateRepository.findByIdForUpdate(6L) } returns early

        // when
        val error = errorOf { service.skip(USER_ID, 5L, late.scheduledChargeAt) }
        service.skip(USER_ID, 6L, early.scheduledChargeAt.minusSeconds(1))

        // then
        assertThat(error).isEqualTo(AutoPickExceptionType.SKIP_CLOSED)
        assertThat(error?.httpStatusCode).isEqualTo(409)
        assertThat(late.status).isEqualTo(SurveyAutoPickStatus.SCHEDULED)
        assertThat(early.status).isEqualTo(SurveyAutoPickStatus.SKIPPED)
    }

    @DisplayName("A1 기존 설정이 있으면 새로 만들지 않고 고친다")
    @Test
    fun updatesExistingSetting() {
        // given
        val setting = SurveyAutoPickSetting(id = 4L, userId = USER_ID, maxPrice = BigDecimal("1000"))
        every { settingRepository.findByUserId(USER_ID) } returns setting

        // when
        service.update(USER_ID, command(maxPrice = "80000"), now)

        // then
        assertThat(setting.enabled).isTrue()
        assertThat(setting.maxPrice).isEqualByComparingTo("80000")
        verify(exactly = 0) { settingRepository.save(any()) }
        verify { productRepository.deleteAllBySettingId(4L) }
    }

    private fun errorOf(block: () -> Unit): CustomExceptionType? =
        runCatching(block).exceptionOrNull()?.let { (it as CustomException).getExceptionType() }

    private fun command(
        enabled: Boolean = true,
        productCount: Int = 2,
        maxPrice: String = "80000",
    ) = SurveyAutoPickCommand(enabled, (1L..productCount.toLong()).toList(), BigDecimal(maxPrice))

    private fun candidate(userId: Long = USER_ID) =
        SurveyAutoPickCandidate(
            userId = userId,
            productId = 1L,
            reservationSlotId = 2L,
            memberOpenAt = now.plusMinutes(5),
            scheduledChargeAt = now.plusMinutes(15),
        )

    private companion object {
        const val USER_ID = 7L
    }
}
