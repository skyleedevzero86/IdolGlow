package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.GlowPassBenefitPolicy
import com.sleekydz86.idolglow.payment.application.MembershipQuery
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickFailReason
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickProduct
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickSetting
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickProductJpaRepository
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickSettingJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("자동 담기 후보 만들기와 결제 시작 조건")
class SurveyAutoPickTxServiceTest {
    private val settingRepository = mockk<SurveyAutoPickSettingJpaRepository>()
    private val productRepository = mockk<SurveyAutoPickProductJpaRepository>()
    private val candidateRepository = mockk<SurveyAutoPickCandidateJpaRepository>()
    private val membershipQuery = mockk<MembershipQuery>()
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val reservationCommandService = mockk<ReservationCommandService>()
    private val counter = mockk<SurveyAutoPickMonthlyCounter>()
    private val notifications = mockk<NotificationCommandService>(relaxed = true)
    private val policy =
        GlowPassBenefitPolicy(
            membershipQuery = membershipQuery,
            publicOpenDaysBefore = 7,
            earlyBookingHours = 24,
            freeCancelDaysMember = 1,
            freeCancelDaysNormal = 3,
        )
    private val service =
        SurveyAutoPickTxService(
            settingRepository,
            productRepository,
            candidateRepository,
            policy,
            membershipQuery,
            methodRepository,
            reservationCommandService,
            counter,
            notifications,
        )
    private val now = LocalDateTime.of(2026, 11, 29, 12, 0)
    private val setting =
        SurveyAutoPickSetting(id = 4L, userId = USER_ID, enabled = true, maxPrice = BigDecimal("80000"))

    @BeforeEach
    fun setUp() {
        every { settingRepository.findByUserId(USER_ID) } returns setting
        every { settingRepository.findAllByEnabledTrue() } returns listOf(setting)
        every { productRepository.findAllBySettingIdIn(listOf(4L)) } returns
            listOf(SurveyAutoPickProduct(settingId = 4L, productId = PRODUCT_ID))
        every { productRepository.existsBySettingIdAndProductId(4L, PRODUCT_ID) } returns true
        every { membershipQuery.isActive(USER_ID, any()) } returns true
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns
            TossBillingMethod(userId = USER_ID, customerKey = "c", billingKey = "bk")
        every { counter.bookedInMonth(USER_ID, now) } returns 0L
        every { reservationCommandService.expectedTotalPrice(PRODUCT_ID, USER_ID, now) } returns BigDecimal("80000.00")
        every { candidateRepository.existsByUserIdAndReservationSlotId(any(), any()) } returns false
        every { candidateRepository.save(any()) } answers { firstArg() }
    }

    @DisplayName("D17 회원 오픈 시각과 같은 때는 후보를 만들고, 오픈 1초 전과 오픈 10분 뒤에는 만들지 않는다")
    @Test
    fun createsCandidatesInsideWindowOnly() {
        // given
        val opensAt = now
        val slot = slot(1L, opensAt.plus(policy.memberOpenLead()))
        every { candidateRepository.findSlotsOfProducts(setOf(PRODUCT_ID), any(), any()) } returns listOf(slot)
        val saved = slot<SurveyAutoPickCandidate>()
        every { candidateRepository.save(capture(saved)) } answers { firstArg() }

        // when
        val beforeOpen = service.createCandidates(opensAt.minusSeconds(1))
        val tenMinutesLater = service.createCandidates(opensAt.plusMinutes(10))
        val atOpen = service.createCandidates(opensAt)

        // then
        assertThat(beforeOpen).isZero()
        assertThat(tenMinutesLater).isZero()
        assertThat(atOpen).isEqualTo(1)
        assertThat(saved.captured.reservationSlotId).isEqualTo(1L)
        assertThat(saved.captured.scheduledChargeAt).isEqualTo(opensAt.plusMinutes(10))
        verify(exactly = 1) {
            notifications.create(USER_ID, NotificationType.AUTO_PICK_SCHEDULED, "자동 담기 결제 예정", any(), "/my-survey")
        }
    }

    @DisplayName("A10 이미 후보가 있는 슬롯은 다시 만들지 않고 알림도 보내지 않는다")
    @Test
    fun skipsExistingCandidate() {
        // given
        every { candidateRepository.findSlotsOfProducts(any(), any(), any()) } returns
            listOf(slot(1L, now.plus(policy.memberOpenLead())))
        every { candidateRepository.existsByUserIdAndReservationSlotId(USER_ID, 1L) } returns true

        // when
        val created = service.createCandidates(now)

        // then
        assertThat(created).isZero()
        verify(exactly = 0) { candidateRepository.save(any()) }
        verify(exactly = 0) { notifications.create(any(), any(), any(), any(), any()) }
    }

    @DisplayName("A7 조건을 모두 채우면 결제 중으로 바꾸고 회원 가격으로 예약 요청을 만든다")
    @Test
    fun claimStartsProcessing() {
        // given
        val candidate = given()

        // when
        val claim = service.claim(CANDIDATE_ID, now)

        // then
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.PROCESSING)
        assertThat(claim?.totalPrice).isEqualByComparingTo("80000")
        assertThat(claim?.reservationSlotId).isEqualTo(SLOT_ID)
    }

    @DisplayName("D14 결제 예정 1초 전 후보와 이미 끝난 후보는 집지 않는다")
    @Test
    fun claimIgnoresEarlyOrClosed() {
        // given
        val early = given(chargeAt = now.plusSeconds(1))

        // when
        val claim = service.claim(CANDIDATE_ID, now)
        early.skip(now)
        val afterSkip = service.claim(CANDIDATE_ID, now.plusMinutes(1))

        // then
        assertThat(claim).isNull()
        assertThat(afterSkip).isNull()
        assertThat(early.status).isEqualTo(SurveyAutoPickStatus.SKIPPED)
    }

    @DisplayName("A8 설정이 꺼졌으면 DISABLED 로 끝낸다")
    @Test
    fun disabledSetting() {
        // given
        setting.update(false, setting.maxPrice)

        // then
        assertBlocked(SurveyAutoPickFailReason.DISABLED)
    }

    @DisplayName("A8 대상 상품에서 빠졌으면 DISABLED 로 끝낸다")
    @Test
    fun productRemoved() {
        // given
        every { productRepository.existsBySettingIdAndProductId(4L, PRODUCT_ID) } returns false

        // then
        assertBlocked(SurveyAutoPickFailReason.DISABLED)
    }

    @DisplayName("A8 회원이 아니면 NOT_MEMBER 로 끝낸다")
    @Test
    fun notMember() {
        // given
        every { membershipQuery.isActive(USER_ID, now) } returns false

        // then
        assertBlocked(SurveyAutoPickFailReason.NOT_MEMBER)
    }

    @DisplayName("A8 기본 카드가 없으면 NO_BILLING_METHOD 로 끝낸다")
    @Test
    fun noCard() {
        // given
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns null

        // then
        assertBlocked(SurveyAutoPickFailReason.NO_BILLING_METHOD)
    }

    @DisplayName("A9 이번 달 자동 예약이 2건이면 MONTHLY_LIMIT 로 끝내고 1건이면 결제한다")
    @Test
    fun monthlyLimitBoundary() {
        // given
        every { counter.bookedInMonth(USER_ID, now) } returns 2L

        // then
        assertBlocked(SurveyAutoPickFailReason.MONTHLY_LIMIT)
        every { counter.bookedInMonth(USER_ID, now) } returns 1L
        val next = given()
        assertThat(service.claim(CANDIDATE_ID, now)).isNotNull
        assertThat(next.status).isEqualTo(SurveyAutoPickStatus.PROCESSING)
    }

    @DisplayName("A8 회원 가격이 예산보다 1원 크면 OVER_BUDGET 이고 같으면 결제한다")
    @Test
    fun budgetBoundary() {
        // given
        every { reservationCommandService.expectedTotalPrice(PRODUCT_ID, USER_ID, now) } returns BigDecimal("80001.00")

        // then
        assertBlocked(SurveyAutoPickFailReason.OVER_BUDGET)
        every { reservationCommandService.expectedTotalPrice(PRODUCT_ID, USER_ID, now) } returns BigDecimal("80000.00")
        given()
        assertThat(service.claim(CANDIDATE_ID, now)).isNotNull
    }

    private fun assertBlocked(reason: SurveyAutoPickFailReason) {
        val candidate = given()
        val claim = service.claim(CANDIDATE_ID, now)
        assertThat(claim).isNull()
        assertThat(candidate.status).isEqualTo(SurveyAutoPickStatus.FAILED)
        assertThat(candidate.failReason).isEqualTo(reason)
    }

    private fun given(chargeAt: LocalDateTime = now): SurveyAutoPickCandidate {
        val candidate =
            SurveyAutoPickCandidate(
                id = CANDIDATE_ID,
                userId = USER_ID,
                productId = PRODUCT_ID,
                reservationSlotId = SLOT_ID,
                memberOpenAt = chargeAt.minusMinutes(10),
                scheduledChargeAt = chargeAt,
            )
        every { candidateRepository.findByIdForUpdate(CANDIDATE_ID) } returns candidate
        return candidate
    }

    private fun slot(
        id: Long,
        startAt: LocalDateTime,
    ): ReservationSlot {
        val product = mockk<Product>(relaxed = true)
        every { product.id } returns PRODUCT_ID
        every { product.name } returns "메이크업 클래스"
        return ReservationSlot(
            id = id,
            product = product,
            reservationDate = startAt.toLocalDate(),
            startTime = startAt.toLocalTime(),
            endTime = startAt.toLocalTime().plusHours(1),
        )
    }

    private companion object {
        const val USER_ID = 7L
        const val PRODUCT_ID = 501L
        const val SLOT_ID = 9001L
        const val CANDIDATE_ID = 30L
    }
}
