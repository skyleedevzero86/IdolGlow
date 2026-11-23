package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationExpirationScheduler
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("토스 승인 검토 PostgreSQL")
class TossConfirmReviewPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var tossPaymentConfirmService: TossPaymentConfirmService

    @Autowired
    private lateinit var tossConfirmReconcileScheduler: TossConfirmReconcileScheduler

    @Autowired
    private lateinit var reservationCommandService: ReservationCommandService

    @Autowired
    private lateinit var reservationExpirationScheduler: ReservationExpirationScheduler

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @DisplayName("토스 대기 중 예약이 만료된 뒤 승인되면 자동 환불한다")
    @Test
    fun refundsWhenTossApprovesAfterReservationExpired() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossDone(created)
        }
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            fullCancel(created.payment.amount, "tx-1")

        // when
        val thread = Thread { confirm(user.id, created, "pk-late") }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        expireReservation(created.id)
        release.countDown()
        thread.join(10_000)

        // then
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertNotEquals(PaymentStatus.EXPIRED, payment.status)
        assertEquals(PaymentStatus.REFUNDED, payment.status)
        val refunds =
            jdbcTemplate.queryForObject(
                "select count(*) from payment_refunds where payment_id = ? and requested_by = 'SYSTEM'",
                Long::class.java,
                created.payment.paymentId,
            )
        assertEquals(1L, refunds)
    }

    @DisplayName("토스 대기 중 만료 스케줄러가 돌아도 예약을 취소하지 않는다")
    @Test
    fun expirationSchedulerSkipsConfirmingReservation() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossFailure()
        }
        // when
        val thread = Thread { runCatching { confirm(user.id, created, "pk-hold") } }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        expireReservation(created.id)
        reservationExpirationScheduler.expirePendingReservations()

        // then
        val reservation = reservationRepository.findById(created.id)!!
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(ReservationStatus.PENDING, reservation.status)
        assertEquals(PaymentStatus.CONFIRMING, payment.status)
        release.countDown()
        thread.join(10_000)
    }

    @DisplayName("토스 대기 중 사용자 취소는 거절한다")
    @Test
    fun userCancelRejectedWhileConfirming() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossFailure()
        }
        val thread = Thread { runCatching { confirm(user.id, created, "pk-cancel") } }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))

        // when
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                reservationCommandService.cancelReservationByUser(created.id, user.id)
            }

        // then
        assertEquals("결제 처리 중입니다. 잠시 후 다시 시도해 주세요.", thrown.message)
        release.countDown()
        thread.join(10_000)
    }

    @DisplayName("예약이 취소된 뒤 토스가 승인하면 예외 대신 자동 환불한다")
    @Test
    fun refundsWhenReservationCanceledBeforeTossSuccess() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            tossDone(created)
        }
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            fullCancel(created.payment.amount, "tx-2")

        // when
        val thread = Thread { confirm(user.id, created, "pk-canceled") }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        jdbcTemplate.update("update reservations set status = 'CANCELED' where id = ?", created.id)
        release.countDown()
        thread.join(10_000)

        // then
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.REFUNDED, payment.status)
    }

    @ParameterizedTest(name = "토스 조회 상태가 {0} 이면 결제 처리 중이 남지 않는다")
    @CsvSource("ABORTED,FAILED", "EXPIRED,FAILED", "CANCELED,FAILED")
    fun reconcileTerminalTossStatusClearsConfirming(
        tossStatus: String,
        expected: PaymentStatus,
    ) {
        // given
        val created = timeoutConfirming()
        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns
            TossApiResponse(
                200,
                objectMapper.readTree("""{"status":"$tossStatus","orderId":"${created.payment.orderId}"}"""),
                """{"status":"$tossStatus"}""",
            )

        // when
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))

        // then
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(expected, payment.status)
        assertNotEquals(PaymentStatus.CONFIRMING, payment.status)
        val reservation = reservationRepository.findById(created.id)!!
        assertEquals(ReservationStatus.CANCELED, reservation.status)
    }

    @DisplayName("토스에 주문이 없으면 결제를 실패로 정리한다")
    @Test
    fun reconcileNotFoundFailsConfirming() {
        // given
        val created = timeoutConfirming()
        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns
            TossApiResponse(404, null, "not found")

        // when
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))

        // then
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.FAILED, payment.status)
    }

    @DisplayName("10분이 지나고 조회가 실패해도 결제를 실패로 만들지 않고 확인 로그를 한 번 남긴다")
    @Test
    fun confirmingIsNotFailedByTimeAlone() {
        // given
        val created = agedConfirming(lookupFailure())

        // when
        reconcile()

        // then
        assertStillConfirming(created)
        assertEquals(1, reviewLogCount(created.payment.paymentId))
        val logged = reviewLog(created.payment.paymentId)
        assertEquals("CONFIRM_ERROR", logged["log_type"])
        assertEquals("SERVER", logged["step"])
        assertEquals(500, (logged["http_status"] as Number).toInt())
        assertEquals("internal", logged["response_body"])
    }

    @DisplayName("10분이 지난 같은 결제의 확인 로그는 다시 조회해도 한 건이다")
    @Test
    fun confirmingStaleIsFlaggedOnce() {
        // given
        val created = agedConfirming(lookupFailure())

        // when
        reconcile()
        reconcile()

        // then
        assertStillConfirming(created)
        assertEquals(1, reviewLogCount(created.payment.paymentId))
    }

    @DisplayName("10분이 지나도 조회 상태가 결론이 아니면 승인 처리 중을 유지한다")
    @Test
    fun confirmingUnresolvedStatusIsNotFailed() {
        // given
        val created =
            agedConfirming(
                TossApiResponse(
                    200,
                    objectMapper.readTree("""{"status":"IN_PROGRESS"}"""),
                    """{"status":"IN_PROGRESS"}""",
                ),
            )

        // when
        reconcile()

        // then
        assertStillConfirming(created)
        assertEquals(1, reviewLogCount(created.payment.paymentId))
    }

    @DisplayName("10분이 지나기 전 조회 실패는 확인 로그를 남기지 않는다")
    @Test
    fun confirmingNotYetStaleIsNotFlagged() {
        // given
        val created = timeoutConfirming()
        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns lookupFailure()

        // when
        reconcile()

        // then
        assertStillConfirming(created)
        assertEquals(0, reviewLogCount(created.payment.paymentId))
    }

    @DisplayName("승인 조회 스케줄은 인자 없는 메서드만 잠근다")
    @Test
    fun confirmReconcileUsesSchedulerLock() {
        val locked = TossConfirmReconcileScheduler::class.java.getMethod("reconcileConfirmingPayments")
        val lock = locked.getAnnotation(SchedulerLock::class.java)
        assertEquals("tossConfirmReconcile", lock.name)
        assertEquals("PT5S", lock.lockAtLeastFor)
        assertEquals("PT1M", lock.lockAtMostFor)
        val direct =
            TossConfirmReconcileScheduler::class.java.getMethod(
                "reconcileConfirmingPayments",
                LocalDateTime::class.java,
            )
        assertNull(direct.getAnnotation(SchedulerLock::class.java))
    }

    @DisplayName("토스 승인 응답이 5xx면 처리 중으로 두고 조회로 맞춘다")
    @Test
    fun fiveXxLeavesConfirmingUntilReconcile() {
        // given
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")

        // when
        tossPaymentConfirmService.confirm(
            user.id,
            "pk-5xx",
            created.payment.orderId,
            created.payment.amount,
            null,
            null,
        )

        // then
        val after5xx = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.CONFIRMING, after5xx.status)

        // when
        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns tossDone(created)
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))

        // then
        val afterReconcile = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.SUCCEEDED, afterReconcile.status)
    }

    private fun agedConfirming(remote: TossApiResponse): ReservationCreatedResponse {
        val created = timeoutConfirming()
        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns remote
        jdbcTemplate.update(
            "update payments set updated_at = ? where id = ?",
            LocalDateTime.now().minusMinutes(11),
            created.payment.paymentId,
        )
        return created
    }

    private fun reconcile() {
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))
    }

    private fun assertStillConfirming(created: ReservationCreatedResponse) {
        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        val reservation = reservationRepository.findById(created.id)!!
        assertEquals(PaymentStatus.CONFIRMING, payment.status)
        assertEquals(ReservationStatus.PENDING, reservation.status)
    }

    private fun reviewLogCount(paymentId: Long): Int =
        jdbcTemplate
            .queryForObject(
                "select count(*) from payment_logs where payment_id = ? and error_message = ?",
                Long::class.java,
                paymentId,
                TossConfirmReconcileScheduler.REVIEW_REQUIRED_MESSAGE,
            )!!
            .toInt()

    private fun reviewLog(paymentId: Long): Map<String, Any?> =
        jdbcTemplate.queryForMap(
            """
            select log_type, step, http_status, response_body
            from payment_logs
            where payment_id = ? and error_message = ?
            """.trimIndent(),
            paymentId,
            TossConfirmReconcileScheduler.REVIEW_REQUIRED_MESSAGE,
        )

    private fun lookupFailure(): TossApiResponse = TossApiResponse(500, null, "internal")

    private fun timeoutConfirming(): ReservationCreatedResponse {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(0, null, "Read timed out", IllegalStateException("Read timed out"))
        confirm(user.id, created, "pk-timeout")
        return created
    }

    private fun confirm(
        userId: Long,
        created: ReservationCreatedResponse,
        paymentKey: String,
    ) {
        tossPaymentConfirmService.confirm(
            userId,
            paymentKey,
            created.payment.orderId,
            created.payment.amount,
            null,
            null,
        )
    }

    private fun expireReservation(reservationId: Long) {
        jdbcTemplate.update(
            "update reservations set expires_at = ? where id = ?",
            LocalDateTime.now().minusMinutes(1),
            reservationId,
        )
    }

    private fun tossDone(created: ReservationCreatedResponse): TossApiResponse =
        TossApiResponse(
            200,
            objectMapper.readTree(
                """
                {"paymentKey":"pk-ok","orderId":"${created.payment.orderId}",
                "status":"DONE","totalAmount":${created.payment.amount.toPlainString()},
                "approvedAt":"2026-10-20T21:30:00+09:00"}
                """.trimIndent().replace("\n", ""),
            ),
            """{"status":"DONE"}""",
        )

    private fun fullCancel(
        amount: java.math.BigDecimal,
        transactionKey: String,
    ): TossApiResponse {
        val plain = amount.stripTrailingZeros().toPlainString()
        val body =
            """{"status":"CANCELED","balanceAmount":0,"totalAmount":$plain,""" +
                """"cancels":[{"transactionKey":"$transactionKey","cancelAmount":$plain}]}"""
        return TossApiResponse(200, objectMapper.readTree(body), body)
    }

    private fun tossFailure(): TossApiResponse =
        TossApiResponse(400, objectMapper.readTree("""{"message":"승인 거절"}"""), """{"message":"승인 거절"}""")
}
