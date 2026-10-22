package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("토스 환불 트랜잭션 분리 PostgreSQL")
class TossRefundOutsideTxPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var reservationCancelFacade: ReservationCancelFacade

    @Autowired
    private lateinit var tossPaymentConfirmService: TossPaymentConfirmService

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

    @DisplayName("환불 HTTP를 기다리는 동안 다른 스레드가 같은 예약을 잠근다")
    @Test
    fun otherThreadCanLockReservationDuringTossRefund() {
        // given
        val created = succeededTossReservation()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossCancel(200, """{"transactionKey":"tx-lock"}""")
        }

        // when
        val thread = Thread { reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소") }
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
    }

    @DisplayName("환불 HTTP가 시간 초과면 결제와 환불 이력은 처리 중으로 남는다")
    @Test
    fun refundTimeoutKeepsRefunding() {
        // given
        val created = succeededTossReservation()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(0, null, "Read timed out")

        // when
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
        assertEquals(ReservationStatus.CANCELED.name, reservationStatus(created.reservationId))
    }

    @DisplayName("환불 HTTP 5xx면 실패로 기록하지 않고 환불 처리 중으로 남긴다")
    @Test
    fun refundFiveXxKeepsRefunding() {
        // given
        val created = succeededTossReservation()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")

        // when
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("환불 HTTP 409 멱등 처리 중이면 환불 처리 중을 유지한다")
    @Test
    fun refundAlreadyProcessingTreatedAsUnknown() {
        // given
        val created = succeededTossReservation()
        val body = """{"code":"IDEMPOTENT_REQUEST_PROCESSING"}"""
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(409, objectMapper.readTree(body), body)

        // when
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")

        // then
        assertRefund(created.paymentId, PaymentStatus.REFUNDING, PaymentRefundStatus.PENDING)
    }

    @DisplayName("환불 HTTP 4xx면 실패 기록과 예약 취소가 남는다")
    @Test
    fun refundFourXxPersistsFailedRecord() {
        // given
        val created = succeededTossReservation()
        val body = """{"code":"REJECT_CARD_COMPANY","message":"거절"}"""
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(400, objectMapper.readTree(body), body)

        // when
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")

        // then
        val payment = paymentJpaRepository.findById(created.paymentId).orElseThrow()
        assertEquals(PaymentStatus.REFUNDING, payment.status)
        val reservation = reservationRepository.findById(created.reservationId)
        assertEquals(ReservationStatus.CANCELED, reservation?.status)
        val refundStatus =
            jdbcTemplate.queryForObject(
                "select status from payment_refunds where payment_id = ?",
                String::class.java,
                created.paymentId,
            )
        assertEquals(PaymentRefundStatus.FAILED.name, refundStatus)
    }

    @DisplayName("환불 처리 중인 예약은 다시 취소할 수 없다")
    @Test
    fun secondCancelRejectedWhileRefunding() {
        // given
        val created = succeededTossReservation()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")
        reservationCancelFacade.cancelByUser(created.reservationId, created.userId, "취소")

        // when
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                reservationCancelFacade.cancelByUser(created.reservationId, created.userId, "다시")
            }

        // then
        assertEquals("환불 처리 중입니다. 잠시 후 다시 시도해 주세요.", thrown.message)
    }

    @DisplayName("늦은 승인 환불 HTTP 동안 예약 잠금을 잡지 않는다")
    @Test
    fun lateApprovalRefundDoesNotHoldLockDuringHttp() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        jdbcTemplate.update("update payments set payment_key = 'pk-late' where id = ?", created.payment.paymentId)
        val confirmEntered = CountDownLatch(1)
        val confirmRelease = CountDownLatch(1)
        val cancelEntered = CountDownLatch(1)
        val cancelRelease = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            confirmEntered.countDown()
            confirmRelease.await(10, TimeUnit.SECONDS)
            tossDone(created)
        }
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } answers {
            cancelEntered.countDown()
            cancelRelease.await(10, TimeUnit.SECONDS)
            tossCancel(200, """{"transactionKey":"tx-late"}""")
        }

        // when
        val thread =
            Thread {
                tossPaymentConfirmService.confirm(
                    user.id,
                    "pk-late",
                    created.payment.orderId,
                    created.payment.amount,
                    null,
                    null,
                )
            }
        thread.start()
        assertTrue(confirmEntered.await(10, TimeUnit.SECONDS))
        jdbcTemplate.update(
            "update reservations set expires_at = ? where id = ?",
            LocalDateTime.now().minusMinutes(1),
            created.id,
        )
        confirmRelease.countDown()
        assertTrue(cancelEntered.await(10, TimeUnit.SECONDS))
        val started = System.nanoTime()
        TransactionTemplate(transactionManager).executeWithoutResult {
            reservationRepository.findByIdForUpdate(created.id)
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        cancelRelease.countDown()
        thread.join(10_000)

        // then
        assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        assertEquals(PaymentStatus.REFUNDED, paymentJpaRepository.findByOrderId(created.payment.orderId)!!.status)
    }

    @DisplayName("환불 멱등 키는 환불 행 번호로 고정된다")
    @Test
    fun refundIdempotencyKeyIsFixedPerRefundRow() {
        // given
        val created = succeededTossReservation()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns TossApiResponse(0, null, "timeout")

        // when
        reservationCancelFacade.cancelMyPagePayment(created.userId, created.paymentId, "취소")

        // then
        val row =
            jdbcTemplate.queryForMap(
                "select id, idempotency_key from payment_refunds where payment_id = ?",
                created.paymentId,
            )
        val refundId = (row["id"] as Number).toLong()
        val key = row["idempotency_key"].toString()
        assertEquals("refund-$refundId", key)
        assertTrue(key.matches(Regex("refund-\\d+")))
    }

    private fun succeededTossReservation(): CreatedPayment {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        jdbcTemplate.update("update payments set payment_key = 'pk-refund' where id = ?", created.payment.paymentId)
        jdbcTemplate.update(
            "update reservations set visit_date = ? where id = ?",
            java.time.LocalDate.now().plusDays(30),
            created.id,
        )
        reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
        return CreatedPayment(user.id, created.id, created.payment.paymentId)
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

    private fun reservationStatus(reservationId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from reservations where id = ?",
            String::class.java,
            reservationId,
        )!!

    private fun tossCancel(
        status: Int,
        body: String,
    ): TossApiResponse = TossApiResponse(status, objectMapper.readTree(body), body)

    private fun tossDone(created: ReservationCreatedResponse): TossApiResponse =
        TossApiResponse(
            200,
            objectMapper.readTree(
                """
                {"paymentKey":"pk-late","orderId":"${created.payment.orderId}","status":"DONE",
                "totalAmount":${created.payment.amount.toPlainString()},"approvedAt":"2026-10-20T21:30:00+09:00"}
                """.trimIndent().replace("\n", ""),
            ),
            """{"status":"DONE"}""",
        )

    private data class CreatedPayment(
        val userId: Long,
        val reservationId: Long,
        val paymentId: Long,
    )
}
