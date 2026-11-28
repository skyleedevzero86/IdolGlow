package com.sleekydz86.idolglow.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

@DisplayName("이용권 변경 차액 계산")
class BillingPlanChangeCalculatorTest {
    private val start = LocalDate.of(2026, 11, 28)
    private val end = LocalDate.of(2027, 2, 28)

    @DisplayName("3개월권을 시작한 날 1년권으로 바꾸면 남은 값 13,748원을 빼고 33,252원을 카드로 낸다")
    @Test
    fun upgradeOnFirstDay() {
        // given
        val today = start

        // when
        val quote = BillingPlanChangeCalculator.quote(start, end, today, 13_900L, 47_000L, 0L)

        // then
        assertThat(quote.periodDays).isEqualTo(92)
        assertThat(quote.remainingDays).isEqualTo(91)
        assertThat(quote.remainingValue).isEqualTo(13_748L)
        assertThat(quote.cardAmount).isEqualTo(33_252L)
        assertThat(quote.creditUsed).isZero()
        assertThat(quote.creditAdded).isZero()
    }

    @DisplayName("남은 일수는 오늘을 빼고 세며 0 아래로 내려가지 않는다")
    @ParameterizedTest(name = "오늘 {0} → 남은 일수 {1}")
    @CsvSource(
        "2027-02-26, 1",
        "2027-02-27, 0",
        "2027-02-28, 0",
        "2027-03-05, 0",
    )
    fun remainingDaysBoundary(
        today: LocalDate,
        expected: Int,
    ) {
        // when
        val remaining = BillingPlanChangeCalculator.remainingDays(today, end)

        // then
        assertThat(remaining).isEqualTo(expected)
    }

    @DisplayName("1년권을 3개월권으로 바꾸면 남은 값에서 새 금액을 뺀 만큼 적립하고 카드 결제는 없다")
    @Test
    fun downgradeAddsCredit() {
        // given
        val yearEnd = LocalDate.of(2027, 11, 28)

        // when
        val quote = BillingPlanChangeCalculator.quote(start, yearEnd, start, 47_000L, 13_900L, 0L)

        // then
        assertThat(quote.periodDays).isEqualTo(365)
        assertThat(quote.remainingValue).isEqualTo(46_871L)
        assertThat(quote.creditAdded).isEqualTo(32_971L)
        assertThat(quote.cardAmount).isZero()
        assertThat(quote.creditUsed).isZero()
    }

    @DisplayName("상향에서 적립금이 정산 금액보다 적으면 적립금을 다 쓰고 나머지만 카드로 낸다")
    @Test
    fun upgradeUsesPartOfCredit() {
        // when
        val quote = BillingPlanChangeCalculator.quote(start, end, start, 13_900L, 47_000L, 10_000L)

        // then
        assertThat(quote.creditUsed).isEqualTo(10_000L)
        assertThat(quote.cardAmount).isEqualTo(23_252L)
    }

    @DisplayName("상향에서 적립금이 정산 금액 이상이면 정산 금액만큼만 쓰고 카드 결제는 없다")
    @Test
    fun upgradeCoveredByCredit() {
        // when
        val quote = BillingPlanChangeCalculator.quote(start, end, start, 13_900L, 47_000L, 50_000L)

        // then
        assertThat(quote.creditUsed).isEqualTo(33_252L)
        assertThat(quote.cardAmount).isZero()
    }

    @DisplayName("카드로 낼 금액이 99원이면 0원, 100원이면 그대로 낸다")
    @ParameterizedTest(name = "적립금 {0} → 카드 {1}")
    @CsvSource(
        "33153, 99, 0",
        "33152, 100, 100",
    )
    fun minimumCardAmount(
        credit: Long,
        beforeWaive: Long,
        expectedCard: Long,
    ) {
        // when
        val quote = BillingPlanChangeCalculator.quote(start, end, start, 13_900L, 47_000L, credit)

        // then
        assertThat(33_252L - quote.creditUsed).isEqualTo(beforeWaive)
        assertThat(quote.cardAmount).isEqualTo(expectedCard)
    }
}
