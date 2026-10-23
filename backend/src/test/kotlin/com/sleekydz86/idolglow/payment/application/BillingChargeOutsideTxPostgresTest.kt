package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("빌링 승인 HTTP는 트랜잭션 밖 PostgreSQL")
class BillingChargeOutsideTxPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var executor: BillingChargeExecutor

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var methodRepository: TossBillingMethodJpaRepository

    @Autowired
    private lateinit var subscriptionRepository: TossBillingSubscriptionJpaRepository

    @Autowired
    private lateinit var chargeRepository: TossBillingChargeJpaRepository

    @Autowired
    private lateinit var attemptRepository: TossBillingChargeAttemptJpaRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        clearMocks(tossPaymentsApiClient)
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
    }

    @DisplayName("승인 HTTP를 기다리는 동안 다른 스레드가 같은 회차를 잠근다")
    @Test
    fun billingHttpOutsideTransaction() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        val orderId = BillingChargeExecutor.periodOrderId(subscriptionId, due)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            cardRejected()
        }

        val thread = Thread { runCatching { executor.chargeSubscription(subscriptionId, due) } }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        val charge = chargeRepository.findByOrderId(orderId)!!
        val started = System.nanoTime()
        TransactionTemplate(transactionManager).executeWithoutResult {
            chargeRepository.findByIdForUpdate(charge.id)
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        release.countDown()
        thread.join(10_000)

        assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        assertTrue(!thread.isAlive)
    }

    @DisplayName("승인 HTTP가 예외로 끊겨도 시도 PENDING이 남고 다시 승인하지 않는다")
    @Test
    fun billingAttemptSavedBeforeHttp() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        val orderId = BillingChargeExecutor.periodOrderId(subscriptionId, due)
        every { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } throws
            IllegalStateException("승인 요청 후 서버가 중단됨")

        val first = runCatching { executor.chargeSubscription(subscriptionId, due) }
        val charge = chargeRepository.findByOrderId(orderId)!!
        val afterFirst = attemptRepository.findAllByChargeIdOrderByAttemptNoAsc(charge.id)

        val second = executor.chargeSubscription(subscriptionId, due)
        val afterSecond = attemptRepository.findAllByChargeIdOrderByAttemptNoAsc(charge.id)

        assertTrue(first.isFailure)
        assertEquals(TossBillingChargeStatus.PENDING, second.status)
        assertEquals(1, afterFirst.size)
        assertEquals(TossBillingChargeStatus.PENDING, afterFirst.first().status)
        assertEquals(1, afterSecond.size)
        verify(exactly = 1) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @DisplayName("같은 회차를 동시에 실행하면 시도와 승인은 한 번이다")
    @Test
    fun concurrentFinalizeCreatesOneAttempt() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = persistPendingCharge(due)
        val calls = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = AtomicInteger()
        every { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            calls.incrementAndGet()
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            cardRejected()
        }
        val first = Thread {
            runCatching { executor.chargeSubscription(seeded.subscriptionId, due) }
            finished.incrementAndGet()
        }
        val second = Thread {
            runCatching { executor.chargeSubscription(seeded.subscriptionId, due) }
            finished.incrementAndGet()
        }

        first.start()
        second.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        waitUntil { finished.get() >= 1 }
        assertTrue(finished.get() >= 1, "finished=${finished.get()} calls=${calls.get()}")
        assertEquals(1, calls.get())
        release.countDown()
        first.join(10_000)
        second.join(10_000)

        val attempts = attemptRepository.findAllByChargeIdOrderByAttemptNoAsc(seeded.chargeId)
        assertEquals(1, attempts.size)
        assertEquals(1, calls.get())
        assertEquals(2, finished.get())
    }

    @DisplayName("조회가 먼저 확정하면 늦게 도착한 승인은 다시 반영하지 않는다")
    @Test
    fun lateApprovalAfterLookupDoesNotChargeTwice() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        val orderId = BillingChargeExecutor.periodOrderId(subscriptionId, due)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            donePayment("pay-late")
        }
        val thread = Thread { runCatching { executor.chargeSubscription(subscriptionId, due) } }
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns donePayment("pay-lookup")
        val lookedUp = executor.chargeSubscription(subscriptionId, due)
        release.countDown()
        thread.join(10_000)

        val subscription = subscriptionRepository.findById(subscriptionId).orElseThrow()
        val charge = chargeRepository.findByOrderId(orderId)!!
        val attempts = attemptRepository.findAllByChargeIdOrderByAttemptNoAsc(charge.id)
        assertEquals(TossBillingChargeStatus.DONE, lookedUp.status)
        assertEquals(due.plusMonths(1), subscription.nextBillingAt)
        assertEquals(TossBillingChargeStatus.DONE, charge.status)
        assertEquals("pay-lookup", charge.paymentKey)
        assertEquals(1, attempts.size)
        assertEquals(TossBillingChargeStatus.DONE, attempts.first().status)
        assertEquals("pay-lookup", attempts.first().paymentKey)
        assertTrue(!thread.isAlive)
        verify(exactly = 1) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @DisplayName("조회가 완료인데 금액이 다르면 다시 승인하지 않는다")
    @Test
    fun billingReconcileAmountMismatchDoesNotApproveAgain() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = persistPendingCharge(due)
        val tossOrderId = BillingChargeExecutor.attemptOrderId(seeded.orderId, 1)
        attemptRepository.save(
            TossBillingChargeAttempt(
                chargeId = seeded.chargeId,
                billingMethod = seeded.method,
                attemptNo = 1,
                tossOrderId = tossOrderId,
                status = TossBillingChargeStatus.FAILED,
            ),
        )
        every { tossPaymentsApiClient.getPaymentByOrderId(tossOrderId) } returns
            TossApiResponse(
                200,
                objectMapper.readTree("""{"status":"DONE","totalAmount":1,"paymentKey":"pay-x"}"""),
                """{"status":"DONE","totalAmount":1}""",
            )

        val result = executor.chargeSubscription(seeded.subscriptionId, due)

        assertEquals(TossBillingChargeStatus.PENDING, result.status)
        assertEquals(1, attemptRepository.findAllByChargeIdOrderByAttemptNoAsc(seeded.chargeId).size)
        verify(exactly = 0) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun donePayment(paymentKey: String): TossApiResponse {
        val body = """{"status":"DONE","totalAmount":4900,"paymentKey":"$paymentKey"}"""
        return TossApiResponse(200, objectMapper.readTree(body), body)
    }

    private fun cardRejected(): TossApiResponse =
        TossApiResponse(
            400,
            objectMapper.readTree("""{"message":"카드사 거절"}"""),
            """{"message":"카드사 거절"}""",
        )

    private fun waitUntil(ready: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!ready() && System.nanoTime() < deadline) {
            Thread.sleep(20)
        }
    }

    private fun persistPendingCharge(due: LocalDateTime): SeededCharge {
        val subscriptionId = persistActiveSubscription(due)
        val subscription = subscriptionRepository.findWithBillingMethodById(subscriptionId)!!
        val orderId = BillingChargeExecutor.periodOrderId(subscriptionId, due)
        val charge =
            chargeRepository.save(
                TossBillingCharge(
                    userId = subscription.userId,
                    subscriptionId = subscription.id,
                    billingMethod = subscription.billingMethod,
                    orderId = orderId,
                    orderName = subscription.orderName,
                    amount = subscription.amount,
                    status = TossBillingChargeStatus.PENDING,
                ),
            )
        return SeededCharge(subscriptionId, charge.id, orderId, subscription.billingMethod)
    }

    private fun persistActiveSubscription(due: LocalDateTime): Long {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val user =
            userJpaRepository.save(
                User(
                    email = "bo$token@test.com",
                    nickname = Nickname.of("o$token"),
                ),
            )
        val method =
            methodRepository.save(
                TossBillingMethod(
                    userId = user.id,
                    customerKey = "idolglow-user-${user.id}",
                    billingKey = "bk-${user.id}",
                    isDefault = true,
                ),
            )
        return subscriptionRepository
            .save(
                TossBillingSubscription(
                    userId = user.id,
                    billingMethod = method,
                    orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                    amount = BillingDefaults.MONTHLY_AMOUNT,
                    nextBillingAt = due,
                ),
            ).id
    }

    private data class SeededCharge(
        val subscriptionId: Long,
        val chargeId: Long,
        val orderId: String,
        val method: TossBillingMethod,
    )
}
