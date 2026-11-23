package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("관리자 환불 재시도 PostgreSQL")
class TossRefundAdminRetryPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var reservationCancelFacade: ReservationCancelFacade

    @Autowired
    private lateinit var paymentRefundService: PaymentRefundService

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @DisplayName("조회가 이미 전액 취소면 재요청 없이 환불을 완료한다")
    @Test
    fun adminRetryChecksTossBeforeResend() {
        // given
        val created = rejectedRefund("pk-admin-canceled")
        stubLookup(created, fullCancel(created.amount, "tx-admin-lookup"))

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        assertEquals(PaymentRefundStatus.SUCCEEDED, refund.status)
        assertEquals("tx-admin-lookup", refund.externalTransactionKey)
        assertEquals(PaymentStatus.REFUNDED, paymentJpaRepository.findById(created.paymentId).orElseThrow().status)
    }

    @DisplayName("확정 거절 뒤 조회가 승인이면 새 멱등 키로 취소를 다시 보낸다")
    @Test
    fun adminRetryResendsAfterConfirmedReject() {
        // given
        val created = rejectedRefund("pk-admin-resend")
        val failedKey = latestRefund(created.paymentId).idempotencyKey
        val sentKeys = mutableListOf<String?>()
        stubLookup(created, doneBody(created.amount))
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } answers {
            val idem = arg<String?>(3)
            if (firstArg<String>() == "pk-admin-resend") {
                sentKeys.add(idem)
                tossCancel(200, fullCancel(created.amount, "tx-admin-resend"))
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        assertEquals(listOf(refund.idempotencyKey), sentKeys)
        assertNotEquals(failedKey, refund.idempotencyKey)
        assertEquals("refund-${refund.id}", refund.idempotencyKey)
        assertEquals(PaymentRefundStatus.SUCCEEDED, refund.status)
        assertEquals("tx-admin-resend", refund.externalTransactionKey)
    }

    @DisplayName("재시도도 거절되면 예외 없이 실패 환불 행을 남긴다")
    @Test
    fun adminRetryRejectedAgainKeepsFailedRecord() {
        // given
        val created = rejectedRefund("pk-admin-reject")
        stubLookup(created, doneBody(created.amount))
        val body = """{"code":"REJECT_CARD_COMPANY","message":"다시 거절"}"""
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(400, objectMapper.readTree(body), body)

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        assertEquals(PaymentRefundStatus.FAILED, refund.status)
        assertEquals(PaymentStatus.REFUNDING, paymentJpaRepository.findById(created.paymentId).orElseThrow().status)
        assertEquals(PaymentRefundStatus.FAILED.name, latestRefund(created.paymentId).status)
        assertEquals(2, refundCount(created.paymentId, PaymentRefundStatus.FAILED))
    }

    @DisplayName("재시도 취소 결과를 모르면 새 환불은 처리 중으로 남는다")
    @Test
    fun adminRetryUnknownResultStaysPending() {
        // given
        val created = rejectedRefund("pk-admin-unknown")
        stubLookup(created, doneBody(created.amount))
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(0, null, "Read timed out")

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        assertEquals(PaymentRefundStatus.PENDING, refund.status)
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("토스 조회가 실패하면 취소를 다시 보내지 않고 처리 중으로 남긴다")
    @Test
    fun adminRetryLookupFailureLeavesPending() {
        // given
        val created = rejectedRefund("pk-admin-lookup-fail")
        every { tossPaymentsApiClient.getPayment(any()) } returns TossApiResponse(500, null, "internal")

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        assertEquals(PaymentRefundStatus.PENDING, refund.status)
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("조회가 4xx이면 본문이 있어도 취소를 다시 보내지 않는다")
    @Test
    fun adminRetryLookupFourXxDoesNotResend() {
        // given
        val created = rejectedRefund("pk-admin-lookup-404")
        val body = """{"code":"NOT_FOUND_PAYMENT","message":"결제 없음"}"""
        every { tossPaymentsApiClient.getPayment(any()) } returns
            TossApiResponse(404, objectMapper.readTree(body), body)

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        assertEquals(PaymentRefundStatus.PENDING, refund.status)
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("조회가 부분 취소면 취소를 다시 보내지 않는다")
    @Test
    fun adminRetryPartialCanceledDoesNotResend() {
        // given
        val created = rejectedRefund("pk-admin-partial")
        val plain = created.amount.stripTrailingZeros().toPlainString()
        val body = """{"status":"PARTIAL_CANCELED","balanceAmount":1,"totalAmount":$plain}"""
        every { tossPaymentsApiClient.getPayment(any()) } returns
            TossApiResponse(200, objectMapper.readTree(body), body)

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        assertEquals(PaymentRefundStatus.PENDING, refund.status)
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("재시도 취소 HTTP를 기다리는 동안 다른 스레드가 같은 예약을 잠근다")
    @Test
    fun adminRetryDoesNotHoldLockDuringToss() {
        // given
        val created = rejectedRefund("pk-admin-lock")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        stubLookup(created, doneBody(created.amount))
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossCancel(200, fullCancel(created.amount, "tx-admin-lock"))
        }

        // when
        val thread = Thread { paymentRefundService.adminRetryLastFailedRefund(created.paymentId) }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        val started = System.nanoTime()
        TransactionTemplate(transactionManager).executeWithoutResult {
            reservationRepository.findByIdForUpdate(created.reservationId)
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        release.countDown()
        thread.join(10_000)

        // then
        assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        assertEquals(PaymentStatus.REFUNDED, paymentJpaRepository.findById(created.paymentId).orElseThrow().status)
    }

    @DisplayName("승인 상태로 남은 예전 실패 환불도 재시도할 수 있다")
    @Test
    fun adminRetryAllowsLegacySucceededPayment() {
        // given
        val created = succeededTossReservation("pk-admin-legacy")
        jdbcTemplate.update(
            """
            insert into payment_refunds (
                payment_id, reservation_id, cancel_amount, cancel_reason, status, requested_by,
                idempotency_key, fail_code, fail_message, created_at, updated_at
            ) values (?, ?, ?, '운영자 취소', 'FAILED', 'ADMIN', ?, 'REJECT_CARD_COMPANY', '거절', now(), now())
            """.trimIndent(),
            created.paymentId,
            created.reservationId,
            created.amount,
            "legacy-${created.paymentId}",
        )
        assertEquals(PaymentStatus.SUCCEEDED, paymentJpaRepository.findById(created.paymentId).orElseThrow().status)
        stubLookup(created, fullCancel(created.amount, "tx-admin-legacy"))

        // when
        val refund = paymentRefundService.adminRetryLastFailedRefund(created.paymentId)

        // then
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
        assertEquals(PaymentRefundStatus.SUCCEEDED, refund.status)
        assertEquals("tx-admin-legacy", refund.externalTransactionKey)
        assertEquals(PaymentStatus.REFUNDED, paymentJpaRepository.findById(created.paymentId).orElseThrow().status)
    }

    private fun rejectedRefund(paymentKey: String): CreatedPayment {
        val created = succeededTossReservation(paymentKey)
        val body = """{"code":"REJECT_CARD_COMPANY","message":"거절"}"""
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(400, objectMapper.readTree(body), body)
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.FAILED)
        clearMocks(tossPaymentsApiClient)
        return created
    }

    private fun stubLookup(
        created: CreatedPayment,
        body: String,
    ) {
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            if (firstArg<String>() == paymentKey(created.paymentId)) {
                tossCancel(200, body)
            } else {
                TossApiResponse(0, null, "다른 건")
            }
        }
    }

    private fun succeededTossReservation(paymentKey: String): CreatedPayment {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        jdbcTemplate.update("update payments set payment_key = ? where id = ?", paymentKey, created.payment.paymentId)
        jdbcTemplate.update(
            "update reservations set visit_date = ? where id = ?",
            java.time.LocalDate.now().plusDays(30),
            created.id,
        )
        reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
        return CreatedPayment(user.id, created.id, created.payment.paymentId, created.payment.amount)
    }

    private fun assertRefund(
        paymentId: Long,
        paymentStatus: PaymentStatus,
        refundStatus: PaymentRefundStatus,
    ) {
        assertEquals(paymentStatus, paymentJpaRepository.findById(paymentId).orElseThrow().status)
        assertEquals(refundStatus.name, latestRefund(paymentId).status)
    }

    private fun paymentKey(paymentId: Long): String =
        jdbcTemplate.queryForObject(
            "select payment_key from payments where id = ?",
            String::class.java,
            paymentId,
        )!!

    private fun latestRefund(paymentId: Long): RefundRow {
        val row =
            jdbcTemplate.queryForMap(
                """
                select id, status, idempotency_key, external_transaction_key
                from payment_refunds where payment_id = ? order by id desc limit 1
                """.trimIndent(),
                paymentId,
            )
        return RefundRow(
            id = (row["id"] as Number).toLong(),
            status = row["status"].toString(),
            idempotencyKey = row["idempotency_key"]?.toString(),
            transactionKey = row["external_transaction_key"]?.toString(),
        )
    }

    private fun refundCount(
        paymentId: Long,
        status: PaymentRefundStatus,
    ): Int =
        jdbcTemplate
            .queryForObject(
                "select count(*) from payment_refunds where payment_id = ? and status = ?",
                Long::class.java,
                paymentId,
                status.name,
            )!!
            .toInt()

    private fun fullCancel(
        amount: BigDecimal,
        transactionKey: String,
    ): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        val head = """{"status":"CANCELED","balanceAmount":0,"totalAmount":$plain,"""
        val tail = """"cancels":[{"transactionKey":"$transactionKey","cancelAmount":$plain}]}"""
        return head + tail
    }

    private fun doneBody(amount: BigDecimal): String {
        val plain = amount.stripTrailingZeros().toPlainString()
        return """{"status":"DONE","balanceAmount":$plain,"totalAmount":$plain}"""
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

    private data class RefundRow(
        val id: Long,
        val status: String,
        val idempotencyKey: String?,
        val transactionKey: String?,
    )
}
