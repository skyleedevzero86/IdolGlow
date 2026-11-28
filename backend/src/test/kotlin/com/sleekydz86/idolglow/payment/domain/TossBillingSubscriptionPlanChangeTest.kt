package com.sleekydz86.idolglow.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("구독 이용권 변경 조건과 적립금")
class TossBillingSubscriptionPlanChangeTest {
    private val due = LocalDateTime.of(2027, 2, 28, 9, 0)
    private val today = LocalDate.of(2026, 11, 28)

    @DisplayName("자동 연장 중이고 실패가 없고 첫 결제를 마친 구독은 다른 기간으로 바꿀 수 있다")
    @Test
    fun allowedWhenAllConditionsMet() {
        // given
        val subscription = subscription()

        // when
        val block = subscription.planChangeBlock(12, today)

        // then
        assertThat(block).isNull()
    }

    @DisplayName("일시정지 구독은 바꿀 수 없다")
    @Test
    fun blockedWhenPaused() {
        // given
        val subscription = subscription(status = TossBillingSubscriptionStatus.PAUSED)

        // when
        val block = subscription.planChangeBlock(12, today)

        // then
        assertThat(block).isEqualTo(PlanChangeBlock.NOT_ACTIVE)
    }

    @DisplayName("해지 예약 구독은 바꿀 수 없다")
    @Test
    fun blockedWhenCancelScheduled() {
        // given
        val subscription = subscription()
        subscription.scheduleCancel(due.minusDays(30))

        // when
        val block = subscription.planChangeBlock(12, today)

        // then
        assertThat(block).isEqualTo(PlanChangeBlock.NOT_ACTIVE)
    }

    @DisplayName("결제 실패가 남은 구독은 바꿀 수 없다")
    @Test
    fun blockedWhenFailureRemains() {
        // given
        val subscription = subscription()
        subscription.markChargeFailed(due)

        // when
        val block = subscription.planChangeBlock(12, today)

        // then
        assertThat(block).isEqualTo(PlanChangeBlock.HAS_FAILURE)
    }

    @DisplayName("첫 결제 전 구독은 바꿀 수 없다")
    @Test
    fun blockedBeforeFirstCharge() {
        // given
        val subscription = subscription(lastChargedAt = null)

        // when
        val block = subscription.planChangeBlock(12, today)

        // then
        assertThat(block).isEqualTo(PlanChangeBlock.NOT_CHARGED_YET)
    }

    @DisplayName("지금과 같은 기간으로는 바꿀 수 없다")
    @Test
    fun blockedForSameCycle() {
        // given
        val subscription = subscription()

        // when
        val block = subscription.planChangeBlock(3, today)

        // then
        assertThat(block).isEqualTo(PlanChangeBlock.SAME_CYCLE)
    }

    @DisplayName("다음 결제일 하루 전에는 바꿀 수 없고 이틀 전에는 바꿀 수 있다")
    @Test
    fun blockedOneDayBeforeDue() {
        // given
        val subscription = subscription()

        // when
        val dayBefore = subscription.planChangeBlock(12, LocalDate.of(2027, 2, 27))
        val twoDaysBefore = subscription.planChangeBlock(12, LocalDate.of(2027, 2, 26))

        // then
        assertThat(dayBefore).isEqualTo(PlanChangeBlock.TOO_CLOSE)
        assertThat(twoDaysBefore).isNull()
    }

    @DisplayName("적립금보다 많이 쓰면 거절한다")
    @Test
    fun rejectsUsingMoreThanCredit() {
        // given
        val subscription = subscription()
        subscription.addCredit(500L)

        // when
        val using = { subscription.useCredit(501L) }

        // then
        assertThatThrownBy(using)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("적립금보다 많이 쓸 수 없습니다")
        assertThat(subscription.creditAmount).isEqualTo(500L)
    }

    @DisplayName("음수 적립은 거절한다")
    @Test
    fun rejectsNegativeCredit() {
        // given
        val subscription = subscription()

        // when
        val adding = { subscription.addCredit(-1L) }

        // then
        assertThatThrownBy(adding)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("적립금은 0 이상만 더할 수 있습니다")
    }

    @DisplayName("이용권을 바꾸면 기간·금액·주문명과 다음 결제일이 새 기간 기준이 된다")
    @Test
    fun changePlanStartsNewPeriod() {
        // given
        val subscription = subscription()
        val now = LocalDateTime.of(2026, 11, 28, 10, 0)

        // when
        subscription.changePlan(BillingCycle.YEARLY, "IdolGlow Glow Pass 1년", now, charged = true)

        // then
        assertThat(subscription.cycleMonths).isEqualTo(12)
        assertThat(subscription.amount).isEqualTo(47_000L)
        assertThat(subscription.orderName).isEqualTo("IdolGlow Glow Pass 1년")
        assertThat(subscription.nextBillingAt).isEqualTo(LocalDateTime.of(2027, 11, 28, 10, 0))
        assertThat(subscription.lastChargedAt).isEqualTo(now)
        assertThat(subscription.status).isEqualTo(TossBillingSubscriptionStatus.ACTIVE)
    }

    private fun subscription(
        status: TossBillingSubscriptionStatus = TossBillingSubscriptionStatus.ACTIVE,
        lastChargedAt: LocalDateTime? = LocalDateTime.of(2026, 11, 28, 9, 0),
    ): TossBillingSubscription =
        TossBillingSubscription(
            userId = 1L,
            billingMethod =
                TossBillingMethod(
                    userId = 1L,
                    customerKey = "idolglow-user-1",
                    billingKey = "billing-key",
                ),
            orderName = "IdolGlow Glow Pass 3개월",
            amount = 13_900L,
            status = status,
            nextBillingAt = due,
            lastChargedAt = lastChargedAt,
            cycleMonths = 3,
        )
}
