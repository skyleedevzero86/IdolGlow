package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingRenewalNoticeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("관리자 Glow Pass 구독 통계 계산")
class AdminBillingStatsServiceTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val noticeRepository = mockk<TossBillingRenewalNoticeJpaRepository>()
    private val service = AdminBillingStatsService(subscriptionRepository, noticeRepository)
    private val now = LocalDateTime.of(2026, 11, 29, 12, 0)

    @DisplayName("S2 자동 연장은 ACTIVE 이고 자동 연장이 켜진 구독만 센다")
    @Test
    fun activeCountsAutoRenewOnly() {
        // given
        given(
            sub(TossBillingSubscriptionStatus.ACTIVE, autoRenew = true),
            sub(TossBillingSubscriptionStatus.ACTIVE, autoRenew = false),
            sub(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, autoRenew = true),
        )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.activeCount).isEqualTo(1)
    }

    @DisplayName("S3 이번 기간만은 CANCEL_SCHEDULED 와 자동 연장이 꺼진 ACTIVE 를 센다")
    @Test
    fun thisPeriodOnlyCountsBoth() {
        // given
        given(
            sub(TossBillingSubscriptionStatus.ACTIVE, autoRenew = true),
            sub(TossBillingSubscriptionStatus.ACTIVE, autoRenew = false),
            sub(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, autoRenew = false),
            sub(TossBillingSubscriptionStatus.PAUSED, autoRenew = false),
        )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.cancelScheduledCount).isEqualTo(2)
    }

    @DisplayName("S4·S5 일시정지는 PAUSED, 재시도 중은 실패 횟수가 있는 ACTIVE 만 센다")
    @Test
    fun pausedAndRetrying() {
        // given
        given(
            sub(TossBillingSubscriptionStatus.PAUSED, failCount = 3),
            sub(TossBillingSubscriptionStatus.ACTIVE, failCount = 1),
            sub(TossBillingSubscriptionStatus.ACTIVE, failCount = 0),
            sub(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, failCount = 2),
        )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.pausedCount).isEqualTo(1)
        assertThat(stats.retryingCount).isEqualTo(1)
    }

    @DisplayName("S6 주기별 구독자는 1·3·6·12개월을 모두 넣고 일시정지 구독은 빼고 센다")
    @Test
    fun byCycleIncludesZeroAndExcludesPaused() {
        // given
        given(
            sub(TossBillingSubscriptionStatus.ACTIVE, cycleMonths = 3),
            sub(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, cycleMonths = 3),
            sub(TossBillingSubscriptionStatus.ACTIVE, cycleMonths = 12),
            sub(TossBillingSubscriptionStatus.PAUSED, cycleMonths = 6),
        )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.byCycle.map { it.cycleMonths to it.count })
            .containsExactly(1 to 0L, 3 to 2L, 6 to 0L, 12 to 1L)
    }

    @DisplayName("S7 환산 월 매출은 구독마다 금액을 개월 수로 나눠 원 미만을 버린 합이다")
    @Test
    fun monthlyRevenueFloorsPerSubscription() {
        // given
        given(
            sub(TossBillingSubscriptionStatus.ACTIVE, cycleMonths = 3, amount = 13_900L),
            sub(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, cycleMonths = 12, amount = 47_000L),
            sub(TossBillingSubscriptionStatus.PAUSED, cycleMonths = 1, amount = 4_900L),
        )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.monthlyRevenue).isEqualTo(4_633L + 3_916L)
    }

    @DisplayName("S8 갱신 안내는 최근 7일 발송분을 계속·이번 기간만·응답 없음으로 나눈다")
    @Test
    fun renewalNoticeSplit() {
        // given
        given()
        every { noticeRepository.findAllBySentAtBetween(now.minusDays(7), now) } returns
            listOf(
                notice(RenewalDecision.CONTINUE),
                notice(RenewalDecision.STOP),
                notice(RenewalDecision.STOP),
                notice(null),
            )

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.renewalNotice.sent).isEqualTo(4)
        assertThat(stats.renewalNotice.continued).isEqualTo(1)
        assertThat(stats.renewalNotice.stopped).isEqualTo(2)
        assertThat(stats.renewalNotice.noAnswer).isEqualTo(1)
    }

    private fun given(vararg subscriptions: TossBillingSubscription) {
        every { subscriptionRepository.findAllByStatusIn(any()) } returns subscriptions.toList()
        every { noticeRepository.findAllBySentAtBetween(any(), any()) } returns emptyList()
    }

    private fun sub(
        status: TossBillingSubscriptionStatus,
        autoRenew: Boolean = true,
        failCount: Int = 0,
        cycleMonths: Int = 1,
        amount: Long = 4_900L,
    ) = TossBillingSubscription(
        userId = 1L,
        billingMethod = TossBillingMethod(userId = 1L, customerKey = "c", billingKey = "bk"),
        orderName = "Glow Pass",
        amount = amount,
        status = status,
        failCount = failCount,
        cycleMonths = cycleMonths,
        autoRenew = autoRenew,
    )

    private fun notice(decision: RenewalDecision?) =
        TossBillingRenewalNotice(
            subscriptionId = 1L,
            userId = 1L,
            periodEnd = now.plusDays(3),
            tokenHash = "h",
            sentAt = now.minusDays(1),
            decision = decision,
        )
}
