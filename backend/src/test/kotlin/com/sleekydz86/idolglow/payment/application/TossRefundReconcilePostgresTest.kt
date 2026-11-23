package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("토스 환불 조회 스케줄러 PostgreSQL")
class TossRefundReconcilePostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var reservationCancelFacade: ReservationCancelFacade

    @Autowired
    private lateinit var tossRefundReconcileScheduler: TossRefundReconcileScheduler

    @Autowired
    private lateinit var tossWebhookService: TossWebhookService

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @DisplayName("환불 HTTP가 시간 초과된 뒤 조회가 전액 취소면 환불 완료로 맞춘다")
    @Test
    fun refundTimeoutKeepsRefundingUntilReconcile() {
        // given
        val created = refundingTossPayment("pk-reconcile-timeout")
        val key = paymentKey(created.paymentId)
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                tossCancel(200, fullCancel(created.amount, "tx-reconcile"))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        tossRefundReconcileScheduler.reconcilePendingRefunds(LocalDateTime.now().plusMinutes(2))

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDED, PaymentRefundStatus.SUCCEEDED)
        assertEquals("tx-reconcile", refundTransactionKey(created.paymentId))
        verify(exactly = 1) { tossPaymentsApiClient.cancel(key, any(), any(), any()) }
    }

    @DisplayName("조회가 아직 승인이면 처음 환불 멱등 키로 취소를 다시 보낸다")
    @Test
    fun reconcileResendsWithSameIdempotencyKey() {
        // given
        val created = refundingTossPayment("pk-reconcile-resend")
        val key = paymentKey(created.paymentId)
        val originalKey = idempotencyKey(created.paymentId)
        val sentKeys = mutableListOf<String?>()
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                tossCancel(200, doneBody(created.amount))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } answers {
            val idem = arg<String?>(3)
            if (firstArg<String>() == key) {
                sentKeys.add(idem)
                tossCancel(200, fullCancel(created.amount, "tx-resend"))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        tossRefundReconcileScheduler.reconcilePendingRefunds(LocalDateTime.now().plusMinutes(2))

        // then
        assertEquals(listOf(originalKey), sentKeys)
        assertRefund(created.paymentId, PaymentStatus.REFUNDED, PaymentRefundStatus.SUCCEEDED)
    }

    @DisplayName("조회가 이미 전액 취소면 취소를 다시 보내지 않는다")
    @Test
    fun reconcileQueriesTossBeforeResend() {
        // given
        val created = refundingTossPayment("pk-reconcile-query-first")
        val key = paymentKey(created.paymentId)
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                tossCancel(200, fullCancel(created.amount, "tx-query-first"))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        tossRefundReconcileScheduler.reconcilePendingRefunds(LocalDateTime.now().plusMinutes(2))

        // then
        verify(exactly = 1) { tossPaymentsApiClient.getPayment(key) }
        verify(exactly = 1) { tossPaymentsApiClient.cancel(key, any(), any(), any()) }
        assertRefund(created.paymentId, PaymentStatus.REFUNDED, PaymentRefundStatus.SUCCEEDED)
    }

    @DisplayName("첫 요청 후 14일이 지나면 취소를 다시 보내지 않고 실패로 바꾸지 않는다")
    @Test
    fun refundAutoRetryStopsAfter14Days() {
        // given
        val created = refundingTossPayment("pk-reconcile-expired")
        val key = paymentKey(created.paymentId)
        jdbcTemplate.update(
            "update payment_refunds set first_requested_at = ? where payment_id = ?",
            LocalDateTime.now().minusDays(15),
            created.paymentId,
        )
        val queries = AtomicInteger()
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                queries.incrementAndGet()
                tossCancel(200, doneBody(created.amount))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        val now = LocalDateTime.now()
        tossRefundReconcileScheduler.reconcilePendingRefunds(now)
        tossRefundReconcileScheduler.reconcilePendingRefunds(now)

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
        verify(exactly = 1) { tossPaymentsApiClient.cancel(key, any(), any(), any()) }
        assertEquals(1, queries.get())
        assertEquals(1, paymentLogCount(created.paymentId, TossRefundReconcileScheduler.RETRY_STOPPED_MESSAGE))
    }

    @DisplayName("조회가 부분 취소면 상태를 그대로 두고 결제 로그만 남긴다")
    @Test
    fun partialCanceledRemoteNotifiesAdmin() {
        // given
        val created = refundingTossPayment("pk-reconcile-partial")
        val key = paymentKey(created.paymentId)
        val body = cancelBody(created.amount, "PARTIAL_CANCELED", "1")
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                tossCancel(200, body)
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        tossRefundReconcileScheduler.reconcilePendingRefunds(LocalDateTime.now().plusMinutes(2))

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
        verify(exactly = 1) { tossPaymentsApiClient.cancel(key, any(), any(), any()) }
        val message = latestCancelError(created.paymentId)
        assertTrue(message.contains(TossRefundReconcileScheduler.UNOWNED_PARTIAL_CANCEL_MESSAGE))
    }

    @DisplayName("환불 처리 중 취소 웹훅은 조회가 전액 취소면 환불을 완료한다")
    @Test
    fun cancelWebhookCompletesRefunding() {
        // given
        val created = refundingTossPayment("pk-reconcile-webhook")
        val key = paymentKey(created.paymentId)
        val orderId = orderId(created.paymentId)
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == key) {
                tossCancel(200, canceledRemote(created.amount, orderId, "tx-webhook"))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }
        val body =
            """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"$orderId","paymentKey":"$key","status":"CANCELED"}}"""

        // when
        tossWebhookService.handlePayload(body)

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDED, PaymentRefundStatus.SUCCEEDED)
        assertEquals("tx-webhook", refundTransactionKey(created.paymentId))
        verify(exactly = 1) { tossPaymentsApiClient.cancel(key, any(), any(), any()) }
    }

    @DisplayName("환불 조회 한 건이 실패해도 다른 건은 맞춘다")
    @Test
    fun reconcileFailureOfOneDoesNotStopOthers() {
        // given
        val failed = refundingTossPayment("pk-reconcile-fail")
        val succeeded = refundingTossPayment("pk-reconcile-ok")
        val failedKey = paymentKey(failed.paymentId)
        val succeededKey = paymentKey(succeeded.paymentId)
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            when (firstArg<String>()) {
                failedKey -> throw IllegalStateException("조회 실패")
                succeededKey -> tossCancel(200, fullCancel(succeeded.amount, "tx-other"))
                else -> TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        tossRefundReconcileScheduler.reconcilePendingRefunds(LocalDateTime.now().plusMinutes(2))

        // then
        assertRefund(failed.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
        assertRefund(succeeded.paymentId, PaymentStatus.REFUNDED, PaymentRefundStatus.SUCCEEDED)
    }

    private fun refundingTossPayment(paymentKey: String): CreatedPayment {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        jdbcTemplate.update("update payments set payment_key = ? where id = ?", paymentKey, created.payment.paymentId)
        jdbcTemplate.update(
            "update reservations set visit_date = ? where id = ?",
            java.time.LocalDate.now().plusDays(30),
            created.id,
        )
        reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(0, null, "Read timed out")
        reservationCancelFacade.cancelMyPagePayment(user.id, created.payment.paymentId, "취소")
        val payment = CreatedPayment(user.id, created.id, created.payment.paymentId, created.payment.amount)
        assertRefund(payment.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
        return payment
    }

    private fun assertRefund(
        paymentId: Long,
        paymentStatus: PaymentStatus,
        refundStatus: PaymentRefundStatus,
    ) {
        val payment = paymentJpaRepository.findById(paymentId).orElseThrow()
        assertEquals(paymentStatus, payment.status)
        val status =
            jdbcTemplate.queryForObject(
                "select status from payment_refunds where payment_id = ?",
                String::class.java,
                paymentId,
            )
        assertEquals(refundStatus.name, status)
    }

    private fun paymentKey(paymentId: Long): String = column("select payment_key from payments where id = ?", paymentId)

    private fun orderId(paymentId: Long): String = column("select order_id from payments where id = ?", paymentId)

    private fun idempotencyKey(paymentId: Long): String =
        column(
            "select idempotency_key from payment_refunds where payment_id = ? and status = 'PENDING'",
            paymentId,
        )

    private fun refundTransactionKey(paymentId: Long): String =
        column("select external_transaction_key from payment_refunds where payment_id = ?", paymentId)

    private fun column(
        sql: String,
        paymentId: Long,
    ): String = jdbcTemplate.queryForObject(sql, String::class.java, paymentId)!!

    private fun paymentLogCount(
        paymentId: Long,
        message: String,
    ): Int =
        jdbcTemplate
            .queryForObject(
                "select count(*) from payment_logs where payment_id = ? and error_message = ?",
                Long::class.java,
                paymentId,
                message,
            )!!
            .toInt()

    private fun latestCancelError(paymentId: Long): String =
        jdbcTemplate.queryForObject(
            """
            select error_message from payment_logs
            where payment_id = ? and log_type = 'CANCEL_ERROR'
            order by id desc
            limit 1
            """.trimIndent(),
            String::class.java,
            paymentId,
        )!!

    private fun fullCancel(
        amount: BigDecimal,
        vararg transactionKeys: String,
    ): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        val cancels =
            transactionKeys.joinToString(",") {
                """{"transactionKey":"$it","cancelAmount":$plain}"""
            }
        return """{"status":"CANCELED","balanceAmount":0,"totalAmount":$plain,"cancels":[$cancels]}"""
    }

    private fun doneBody(amount: BigDecimal): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        return """{"status":"DONE","balanceAmount":$plain,"totalAmount":$plain}"""
    }

    private fun cancelBody(
        amount: BigDecimal,
        status: String,
        balanceAmount: String,
    ): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        return """{"status":"$status","balanceAmount":$balanceAmount,"totalAmount":$plain}"""
    }

    private fun canceledRemote(
        amount: BigDecimal,
        orderId: String,
        transactionKey: String,
    ): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        val head = """{"status":"CANCELED","balanceAmount":0,"totalAmount":$plain,"orderId":"$orderId","""
        val tail = """"cancels":[{"transactionKey":"$transactionKey","cancelAmount":$plain}]}"""
        return head + tail
    }

    private fun tossCancel(
        status: Int,
        body: String,
    ): TossApiResponse = TossApiResponse(status, objectMapper.readTree(body), body)

    private data class CreatedPayment(
        val userId: Long,
        val reservationId: Long,
        val paymentId: Long,
        val amount: BigDecimal,
    )
}
