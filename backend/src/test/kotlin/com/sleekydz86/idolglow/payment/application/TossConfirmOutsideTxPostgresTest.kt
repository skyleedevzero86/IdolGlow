package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("토스 승인 트랜잭션 분리 PostgreSQL")
class TossConfirmOutsideTxPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var tossPaymentConfirmService: TossPaymentConfirmService

    @Autowired
    private lateinit var tossConfirmReconcileScheduler: TossConfirmReconcileScheduler

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @DisplayName("토스 호출 중에는 같은 예약을 짧은 시간에 잠글 수 있다")
    @Test
    fun otherThreadCanLockReservationDuringTossCall() {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        val enteredConfirm = CountDownLatch(1)
        val releaseConfirm = CountDownLatch(1)
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } answers {
            enteredConfirm.countDown()
            releaseConfirm.await(10, TimeUnit.SECONDS)
            tossFailure()
        }
        val confirmThread =
            Thread {
                runCatching {
                    tossPaymentConfirmService.confirm(
                        user.id,
                        "pk-1",
                        created.payment.orderId,
                        created.payment.amount,
                        null,
                        null,
                    )
                }
            }
        confirmThread.start()
        assertTrue(enteredConfirm.await(10, TimeUnit.SECONDS))
        val elapsedMs =
            TransactionTemplate(transactionManager).execute {
                val started = System.nanoTime()
                reservationRepository.findByIdForUpdate(created.id)
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            }!!
        releaseConfirm.countDown()
        confirmThread.join(10_000)
        assertTrue(elapsedMs < 1000, "elapsedMs=$elapsedMs")
    }

    @DisplayName("토스 승인 실패면 실패 기록이 남는다")
    @Test
    fun failedConfirmPersistsFailedStatus() {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns tossFailure()

        tossPaymentConfirmService.confirm(
            user.id,
            "pk-fail",
            created.payment.orderId,
            created.payment.amount,
            null,
            null,
        )

        val payment = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.FAILED, payment.status)
    }

    @DisplayName("토스 타임아웃이면 처리 중으로 남고 조회 스케줄러가 DONE 이면 성공으로 맞춘다")
    @Test
    fun timeoutStaysConfirmingUntilReconcileFindsDone() {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(0, null, "Read timed out", IllegalStateException("Read timed out"))

        tossPaymentConfirmService.confirm(
            user.id,
            "pk-timeout",
            created.payment.orderId,
            created.payment.amount,
            null,
            null,
        )
        val afterTimeout = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.CONFIRMING, afterTimeout.status)

        every { tossPaymentsApiClient.getPaymentByOrderId(created.payment.orderId) } returns
            TossApiResponse(
                200,
                objectMapper.readTree(
                    """
                    {"paymentKey":"pk-timeout","orderId":"${created.payment.orderId}",
                    "status":"DONE","totalAmount":${created.payment.amount.toPlainString()},
                    "approvedAt":"2026-10-20T21:30:00+09:00"}
                    """.trimIndent().replace("\n", ""),
                ),
                """{"status":"DONE"}""",
            )
        tossConfirmReconcileScheduler.reconcileConfirmingPayments(LocalDateTime.now().plusMinutes(1))

        val afterReconcile = paymentJpaRepository.findByOrderId(created.payment.orderId)!!
        assertEquals(PaymentStatus.SUCCEEDED, afterReconcile.status)
    }

    private fun tossFailure(): TossApiResponse =
        TossApiResponse(400, objectMapper.readTree("""{"message":"승인 거절"}"""), """{"message":"승인 거절"}""")
}
