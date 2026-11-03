package com.sleekydz86.idolglow.payment.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("자동결제 회차 취소")
class TossBillingChargeCancelTest {
    private val approvedAt = LocalDateTime.of(2026, 11, 3, 9, 0)

    @DisplayName("결제일로부터 14일째까지는 취소할 수 있다")
    @Test
    fun cancelableWithin14Days() {
        // given
        val charge = doneCharge()

        // when, then
        assertDoesNotThrow { charge.assertCancelable(approvedAt.plusDays(14)) }
        assertEquals(approvedAt.plusDays(14), charge.cancelDeadline())
    }

    @DisplayName("결제일로부터 14일이 지나면 취소할 수 없다")
    @Test
    fun rejectsAfter14Days() {
        // given
        val charge = doneCharge()

        // when
        val afterDeadline = approvedAt.plusDays(14).plusSeconds(1)
        val error = assertThrows<IllegalArgumentException> { charge.assertCancelable(afterDeadline) }

        // then
        assertEquals(true, error.message!!.startsWith("카드 결제는 결제일로부터 14일이 지나면"))
    }

    @DisplayName("결제 완료가 아닌 회차는 취소할 수 없고, 취소하면 상태와 시각이 남는다")
    @Test
    fun onlyDoneCanBeCanceled() {
        // given
        val charge = doneCharge()
        val canceledAt = approvedAt.plusDays(1)

        // when
        charge.markCanceled(canceledAt)

        // then
        assertEquals(TossBillingChargeStatus.CANCELED, charge.status)
        assertEquals(canceledAt, charge.canceledAt)
        assertThrows<IllegalArgumentException> { charge.assertCancelable(canceledAt) }
    }

    private fun doneCharge(): TossBillingCharge {
        val method =
            TossBillingMethod(
                userId = 1L,
                customerKey = "idolglow-user-1",
                billingKey = "billing-key",
            )
        return TossBillingCharge(
            userId = 1L,
            subscriptionId = 10L,
            billingMethod = method,
            orderId = "sub-10-202611",
            orderName = "IdolGlow Glow Pass 3개월",
            amount = 13_900L,
        ).also { it.markDone(paymentKey = "pay-key", raw = null, approvedAt = approvedAt) }
    }
}
