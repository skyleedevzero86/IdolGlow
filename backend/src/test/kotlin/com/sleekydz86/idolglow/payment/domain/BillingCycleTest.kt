package com.sleekydz86.idolglow.payment.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("결제 주기")
class BillingCycleTest {
    @DisplayName("개월 수로 결제 주기와 금액을 찾는다")
    @Test
    fun findsCycleByMonths() {
        assertEquals(BillingCycle.MONTHLY, BillingCycle.ofMonths(1))
        assertEquals(13_900L, BillingCycle.ofMonths(3).amount)
        assertEquals(25_900L, BillingCycle.ofMonths(6).amount)
        assertEquals(47_000L, BillingCycle.ofMonths(12).amount)
    }

    @DisplayName("지원하지 않는 개월 수는 거절한다")
    @Test
    fun rejectsUnknownMonths() {
        val error = assertThrows<IllegalArgumentException> { BillingCycle.ofMonths(2) }

        assertEquals("지원하지 않는 결제 주기입니다. cycleMonths=2", error.message)
    }
}
