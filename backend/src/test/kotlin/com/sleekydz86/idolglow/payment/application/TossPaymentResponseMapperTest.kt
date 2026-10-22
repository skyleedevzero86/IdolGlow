package com.sleekydz86.idolglow.payment.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@DisplayName("토스 결제 응답")
class TossPaymentResponseMapperTest {
    private val objectMapper = ObjectMapper()
    private val amount = BigDecimal("35000")

    @DisplayName("전액 취소는 CANCELED 이고 잔액 0 이며 총액이 기대 금액과 같다")
    @Test
    fun isFullyCanceledOnlyWhenBalanceIsZero() {
        // given
        val full = objectMapper.readTree("""{"status":"CANCELED","balanceAmount":0,"totalAmount":35000}""")
        val partial = objectMapper.readTree("""{"status":"PARTIAL_CANCELED","balanceAmount":0,"totalAmount":35000}""")
        val leftover = objectMapper.readTree("""{"status":"CANCELED","balanceAmount":10,"totalAmount":35000}""")

        // when
        val fullResult = TossPaymentResponseMapper.isFullyCanceled(full, amount)
        val partialResult = TossPaymentResponseMapper.isFullyCanceled(partial, amount)
        val leftoverResult = TossPaymentResponseMapper.isFullyCanceled(leftover, amount)

        // then
        assertTrue(fullResult)
        assertFalse(partialResult)
        assertFalse(leftoverResult)
    }

    @DisplayName("거래 키는 cancels 마지막 항목이고 없으면 lastTransactionKey 다")
    @Test
    fun cancelTransactionKeyPrefersLastCancel() {
        // given
        val cancels =
            objectMapper.readTree(
                """{"cancels":[{"transactionKey":"tx-a"},{"transactionKey":"tx-b"}],"lastTransactionKey":"tx-root"}""",
            )
        val fallback = objectMapper.readTree("""{"cancels":[],"lastTransactionKey":"tx-fallback"}""")
        val missing = objectMapper.readTree("""{"status":"CANCELED"}""")

        // when
        val fromCancels = TossPaymentResponseMapper.cancelTransactionKey(cancels)
        val fromFallback = TossPaymentResponseMapper.cancelTransactionKey(fallback)
        val fromMissing = TossPaymentResponseMapper.cancelTransactionKey(missing)

        // then
        assertEquals("tx-b", fromCancels)
        assertEquals("tx-fallback", fromFallback)
        assertNull(fromMissing)
    }
}
