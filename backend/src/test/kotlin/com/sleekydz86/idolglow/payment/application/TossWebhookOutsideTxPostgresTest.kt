package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCancelFacade
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.User
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@DisplayName("토스 웹훅 조회 잠금 분리 PostgreSQL")
class TossWebhookOutsideTxPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var tossWebhookService: TossWebhookService

    @Autowired
    private lateinit var reservationCancelFacade: ReservationCancelFacade

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @DisplayName("웹훅이 토스 조회를 기다리는 동안 다른 스레드가 같은 예약을 잠근다")
    @Test
    fun webhookDoesNotHoldLockDuringTossLookup() {
        val created = pendingReservation()
        val held = holdLookup(created)
        val failure = AtomicReference<Throwable>()
        val thread = Thread { tossWebhookService.handlePayload(webhookBody(created)) }
        thread.setUncaughtExceptionHandler { _, error -> failure.set(error) }
        thread.start()
        try {
            assertTrue(held.entered.await(10, TimeUnit.SECONDS))
            val started = System.nanoTime()
            TransactionTemplate(transactionManager).executeWithoutResult {
                reservationRepository.findByIdForUpdate(created.id)
            }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        } finally {
            held.release.countDown()
            thread.join(10_000)
        }
        assertFalse(thread.isAlive)
        assertNull(failure.get())
    }

    @DisplayName("웹훅이 토스 조회를 기다리는 동안 같은 예약의 사용자 취소가 1초 안에 끝난다")
    @Test
    fun cancelDoesNotWaitForWebhookTossLookup() {
        val user = fixture.persistUser()
        val created = pendingReservation(user)
        val held = holdLookup(created)
        val failure = AtomicReference<Throwable>()
        val thread = Thread { tossWebhookService.handlePayload(webhookBody(created)) }
        thread.setUncaughtExceptionHandler { _, error -> failure.set(error) }
        thread.start()
        try {
            assertTrue(held.entered.await(10, TimeUnit.SECONDS))
            val started = System.nanoTime()
            reservationCancelFacade.cancelByUser(created.id, user.id, null)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        } finally {
            held.release.countDown()
            thread.join(10_000)
        }
        assertFalse(thread.isAlive)
        assertNull(failure.get())
        assertEquals(PaymentStatus.CANCELED.name, paymentStatus(created.payment.paymentId))
        assertEquals(ReservationStatus.CANCELED.name, reservationStatus(created.id))
    }

    @DisplayName("조회 중 사용자 취소가 결제를 바꾸면 이후 DONE을 승인으로 만들지 않는다")
    @Test
    fun webhookRechecksStateAfterLock() {
        val user = fixture.persistUser()
        val created = pendingReservation(user)
        val held = holdLookup(created)
        val failure = AtomicReference<Throwable>()
        val thread = Thread { tossWebhookService.handlePayload(webhookBody(created)) }
        thread.setUncaughtExceptionHandler { _, error -> failure.set(error) }
        thread.start()
        try {
            assertTrue(held.entered.await(10, TimeUnit.SECONDS))
            reservationCancelFacade.cancelByUser(created.id, user.id, null)
        } finally {
            held.release.countDown()
            thread.join(10_000)
        }
        assertFalse(thread.isAlive)
        assertNull(failure.get())
        assertEquals(PaymentStatus.CANCELED.name, paymentStatus(created.payment.paymentId))
        assertEquals(1, logCount(created.payment.paymentId, "종료된 결제 상태: CANCELED"))
    }

    @DisplayName("같은 DONE 웹훅을 두 번 받아도 승인은 한 번이고 두 번째는 멱등 로그만 남긴다")
    @Test
    fun duplicateWebhookIsIdempotent() {
        val created = pendingReservation()
        every { tossPaymentsApiClient.getPayment(any()) } returns doneResponse(created)
        val body = webhookBody(created)

        tossWebhookService.handlePayload(body)
        tossWebhookService.handlePayload(body)

        assertEquals(PaymentStatus.SUCCEEDED.name, paymentStatus(created.payment.paymentId))
        assertEquals(1, logCount(created.payment.paymentId, "이미 승인됨(멱등)"))
        verify(exactly = 1) { tossPaymentsApiClient.getPayment(any()) }
    }

    private fun pendingReservation(user: User = fixture.persistUser()): ReservationCreatedResponse =
        fixture.createReservation(user.id, fixture.persistOpenSlot())

    private fun holdLookup(created: ReservationCreatedResponse): HeldLookup {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.getPayment(any()) } answers {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS)) { "웹훅 조회 대기가 풀리지 않았습니다." }
            doneResponse(created)
        }
        return HeldLookup(entered, release)
    }

    private fun webhookBody(created: ReservationCreatedResponse): String =
        """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"${created.payment.orderId}","paymentKey":"$PAYMENT_KEY","status":"DONE"}}"""

    private fun doneResponse(created: ReservationCreatedResponse): TossApiResponse {
        val body =
            """{"orderId":"${created.payment.orderId}","paymentKey":"$PAYMENT_KEY","status":"DONE","totalAmount":${created.payment.amount.toPlainString()}}"""
        return TossApiResponse(httpStatus = 200, json = objectMapper.readTree(body), rawBody = body)
    }

    private fun paymentStatus(paymentId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from payments where id = ?",
            String::class.java,
            paymentId,
        )!!

    private fun reservationStatus(reservationId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from reservations where id = ?",
            String::class.java,
            reservationId,
        )!!

    private fun logCount(
        paymentId: Long,
        errorMessage: String,
    ): Int =
        jdbcTemplate
            .queryForObject(
                "select count(*) from payment_logs where payment_id = ? and error_message = ?",
                Long::class.java,
                paymentId,
                errorMessage,
            )!!
            .toInt()

    private data class HeldLookup(
        val entered: CountDownLatch,
        val release: CountDownLatch,
    )

    companion object {
        private const val PAYMENT_KEY = "pk-webhook"
    }
}
