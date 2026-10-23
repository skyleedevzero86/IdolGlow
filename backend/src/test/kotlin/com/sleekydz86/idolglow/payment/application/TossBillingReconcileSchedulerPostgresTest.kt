package com.sleekydz86.idolglow.payment.application

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("빌링 승인 조회 스케줄 PostgreSQL")
@Suppress("TooManyFunctions")
class TossBillingReconcileSchedulerPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var scheduler: TossBillingReconcileScheduler

    @Autowired
    private lateinit var billingChargeTxService: BillingChargeTxService

    @Autowired
    private lateinit var billingChargeReconcileTxService: BillingChargeReconcileTxService

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
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockkBean(relaxed = true)
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    @MockkBean(relaxed = true)
    private lateinit var notificationCommandService: NotificationCommandService

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        clearMocks(tossPaymentsApiClient, notificationCommandService)
    }

    @DisplayName("조회가 완료이고 금액이 같으면 정기구독 시도와 회차와 구독을 성공 처리한다")
    @Test
    fun billingReconcileMarksDoneWhenTossDone() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = seedPending(due, oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns doneRemote("pay-sub")

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        val attempt = attemptRepository.findById(seeded.attemptId).orElseThrow()
        val subscription = subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow()
        val approvedAt = LocalDateTime.of(2026, 11, 1, 0, 0)
        assertEquals(TossBillingChargeStatus.DONE, charge.status)
        assertEquals("pay-sub", charge.paymentKey)
        assertEquals(approvedAt, charge.approvedAt)
        assertTrue(charge.rawResponse!!.contains("pay-sub"))
        assertEquals(TossBillingChargeStatus.DONE, attempt.status)
        assertEquals("pay-sub", attempt.paymentKey)
        assertEquals(approvedAt, attempt.approvedAt)
        assertTrue(attempt.rawResponse!!.contains("pay-sub"))
        assertEquals(due.plusMonths(1), subscription.nextBillingAt)
        assertEquals(0, subscription.failCount)
        verify(exactly = 0) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @DisplayName("구독이 없는 단건 결제도 조회가 완료면 DONE이 된다")
    @Test
    fun billingOneOffReconcileMarksDoneWhenTossDone() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = true)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns doneRemote("pay-once")

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        val attempt = attemptRepository.findById(seeded.attemptId).orElseThrow()
        assertEquals(TossBillingChargeStatus.DONE, charge.status)
        assertEquals("pay-once", charge.paymentKey)
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), charge.approvedAt)
        assertTrue(charge.rawResponse!!.contains("pay-once"))
        assertEquals(TossBillingChargeStatus.DONE, attempt.status)
        assertNull(charge.subscriptionId)
    }

    @DisplayName("ABORTED·EXPIRED·404만 실패로 두고 결과 모름과 비종료 상태는 대기한다")
    @Test
    fun billingReconcileFailsOnlyOnTerminalStatus() {
        // given
        val cases =
            listOf(
                RemoteCase(200, "ABORTED", null, true),
                RemoteCase(200, "EXPIRED", null, true),
                RemoteCase(404, null, null, true),
                RemoteCase(0, null, null, false),
                RemoteCase(500, null, null, false),
                RemoteCase(409, null, "IDEMPOTENT_REQUEST_PROCESSING", false),
                RemoteCase(200, "IN_PROGRESS", null, false),
                RemoteCase(200, "CANCELED", null, false),
                RemoteCase(400, null, "INVALID_REQUEST", false),
            )

        // when
        cases.forEach { item -> assertTerminalCase(item) }
    }

    @DisplayName("조회가 완료여도 금액이 다르면 시도와 회차는 대기로 남는다")
    @Test
    fun billingReconcileAmountMismatchKeepsPending() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns
            statusRemote(200, "DONE", null, totalAmount = 1)

        // when
        val logs = captureLogs {
            scheduler.reconcilePendingBillingAttempts(agedNow())
        }

        // then
        assertStillPending(seeded)
        assertEquals(0, subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow().failCount)
        assertTrue(logs.any { it.contains("금액이 회차 금액과 다릅니다") && it.contains("chargeId=${seeded.chargeId}") })
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("같은 종료 응답을 다시 처리해도 실패 횟수와 알림은 한 번이다")
    @Test
    fun billingReconcileDoesNotDuplicateFailure() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns
            statusRemote(200, "ABORTED", null)
        val now = agedNow()

        // when
        scheduler.reconcilePendingBillingAttempts(now)
        scheduler.reconcilePendingBillingAttempts(now)

        // then
        assertEquals(TossBillingChargeStatus.FAILED, chargeRepository.findById(seeded.chargeId).orElseThrow().status)
        assertEquals(1, subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow().failCount)
        verify(exactly = 1) {
            notificationCommandService.create(
                any(),
                NotificationType.PAYMENT_FAILED,
                "Glow Pass 결제 실패",
                match { it.startsWith("자동결제에 실패했습니다.") },
                "/billing",
            )
        }
    }

    @DisplayName("단건 결제 404·ABORTED는 일반 결제 실패 문구를 보낸다")
    @Test
    fun billingOneOffReconcileFailureUsesPlainPaymentCopy() {
        // given
        val cases =
            listOf(
                RemoteCase(404, null, null, true),
                RemoteCase(200, "ABORTED", null, true),
            )

        // when
        cases.forEach { item -> assertOneOffFailureNotice(item) }
    }

    @DisplayName("조회 중에 끝난 시도와 회차는 늦은 응답으로 덮지 않는다")
    @Test
    fun billingReconcileDoesNotOverwriteSettledAttempt() {
        assertLateFailureDoesNotUndoDone()
        assertLateDoneDoesNotUndoFailure()
    }

    @DisplayName("1시간 넘게 결과를 모르는 시도는 확인 로그만 남기고 실패로 바꾸지 않는다")
    @Test
    fun billingReconcileLogsPendingAfterOneHour() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        val createdAt = LocalDateTime.of(2026, 10, 23, 15, 0)
        jdbcTemplate.update(
            "update toss_billing_charge_attempts set created_at = ? where id = ?",
            Timestamp.valueOf(createdAt),
            seeded.attemptId,
        )
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns TossApiResponse(500, null, null)

        // when
        val logs = captureLogs {
            scheduler.reconcilePendingBillingAttempts(agedNow())
        }

        // then
        assertStillPending(seeded)
        assertEquals(0, subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow().failCount)
        val review = logs.first { it.contains("1시간 넘게 확인되지 않음") }
        assertTrue(review.contains("chargeId=${seeded.chargeId}"))
        assertTrue(review.contains("attemptId=${seeded.attemptId}"))
        assertTrue(review.contains("tossOrderId=${seeded.tossOrderId}"))
        assertTrue(review.contains("createdAt=$createdAt"))
        assertTrue(review.contains("httpStatus=500"))
        assertTrue(!review.contains("bk-"))
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("토스 조회 중에는 회차 행을 잠그지 않는다")
    @Test
    fun billingReconcileDoesNotHoldLockDuringTossLookup() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            TossApiResponse(500, null, null)
        }
        val thread = Thread { scheduler.reconcilePendingBillingAttempts(agedNow()) }

        // when
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        val started = System.nanoTime()
        TransactionTemplate(transactionManager).executeWithoutResult {
            chargeRepository.findByIdForUpdate(seeded.chargeId)
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        release.countDown()
        thread.join(10_000)

        // then
        assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        assertTrue(!thread.isAlive)
    }

    @DisplayName("만든 지 1분이 안 된 시도는 조회하지 않는다")
    @Test
    fun billingReconcileSkipsAttemptYoungerThanOneMinute() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns doneRemote("pay-young")

        // when
        scheduler.reconcilePendingBillingAttempts(LocalDateTime.now())

        // then
        assertStillPending(seeded)
        verify(exactly = 0) { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) }
    }

    @DisplayName("끝난 과거 시도는 다시 조회하지 않고 최신 PENDING 시도만 본다")
    @Test
    fun billingReconcileLooksUpOnlyLatestPendingAttempt() {
        // given
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        val previous = attemptRepository.findById(seeded.attemptId).orElseThrow()
        previous.markFailed("이전 거절", null)
        attemptRepository.save(previous)
        val latestOrderId = saveAttempt(seeded, attemptNo = 2, status = TossBillingChargeStatus.PENDING)
        every { tossPaymentsApiClient.getPaymentByOrderId(latestOrderId) } returns TossApiResponse(500, null, null)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns doneRemote("pay-old")

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        assertEquals(TossBillingChargeStatus.FAILED, attemptRepository.findById(seeded.attemptId).orElseThrow().status)
        assertEquals(TossBillingChargeStatus.PENDING, chargeRepository.findById(seeded.chargeId).orElseThrow().status)
        verify(exactly = 0) { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) }
        verify(exactly = 1) { tossPaymentsApiClient.getPaymentByOrderId(latestOrderId) }
    }

    @DisplayName("회차가 이미 끝났으면 남은 시도는 조회하지 않는다")
    @Test
    fun billingReconcileSkipsWhenChargeIsNotPending() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = seedPending(due, oneOff = false)
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        charge.markDone("pay-already", null, due)
        chargeRepository.save(charge)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns
            TossApiResponse(404, null, null)

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        assertEquals(TossBillingChargeStatus.PENDING, attemptRepository.findById(seeded.attemptId).orElseThrow().status)
        assertEquals(TossBillingChargeStatus.DONE, chargeRepository.findById(seeded.chargeId).orElseThrow().status)
        verify(exactly = 0) { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) }
    }

    @DisplayName("한 건 조회가 예외여도 다음 PENDING 시도는 계속 처리한다")
    @Test
    fun billingReconcileContinuesAfterOneLookupFails() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val failedLookup = seedPending(due, oneOff = false)
        val next = seedPending(due, oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(failedLookup.tossOrderId) } throws
            IllegalStateException("조회 중단")
        every { tossPaymentsApiClient.getPaymentByOrderId(next.tossOrderId) } returns doneRemote("pay-next")

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        assertStillPending(failedLookup)
        assertEquals(TossBillingChargeStatus.DONE, chargeRepository.findById(next.chargeId).orElseThrow().status)
        assertEquals("pay-next", attemptRepository.findById(next.attemptId).orElseThrow().paymentKey)
    }

    private fun assertTerminalCase(item: RemoteCase) {
        // given
        clearMocks(tossPaymentsApiClient, notificationCommandService)
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = false)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns
            statusRemote(item.httpStatus, item.status, item.code)

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        val attempt = attemptRepository.findById(seeded.attemptId).orElseThrow()
        val failCount = subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow().failCount
        val expected = if (item.fails) TossBillingChargeStatus.FAILED else TossBillingChargeStatus.PENDING
        assertEquals(expected, charge.status)
        assertEquals(expected, attempt.status)
        assertEquals(if (item.fails) 1 else 0, failCount)
        verify(exactly = if (item.fails) 1 else 0) {
            notificationCommandService.create(
                any(),
                NotificationType.PAYMENT_FAILED,
                "Glow Pass 결제 실패",
                match { it.startsWith("자동결제에 실패했습니다.") },
                "/billing",
            )
        }
    }

    private fun assertOneOffFailureNotice(item: RemoteCase) {
        // given
        clearMocks(tossPaymentsApiClient, notificationCommandService)
        val seeded = seedPending(LocalDateTime.of(2026, 11, 1, 0, 0), oneOff = true)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } returns
            statusRemote(item.httpStatus, item.status, item.code)

        // when
        scheduler.reconcilePendingBillingAttempts(agedNow())

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        val attempt = attemptRepository.findById(seeded.attemptId).orElseThrow()
        assertEquals(TossBillingChargeStatus.FAILED, charge.status)
        assertEquals(TossBillingChargeStatus.FAILED, attempt.status)
        assertNull(charge.subscriptionId)
        verify(exactly = 1) {
            notificationCommandService.create(
                charge.userId,
                NotificationType.PAYMENT_FAILED,
                "결제 실패",
                match { body ->
                    body.startsWith("결제에 실패했습니다.") &&
                        !body.contains("자동결제") &&
                        !body.contains("Glow Pass")
                },
                "/billing",
            )
        }
    }

    private fun assertLateFailureDoesNotUndoDone() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = seedPending(due, oneOff = false)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            TossApiResponse(404, null, null)
        }
        val thread = Thread { scheduler.reconcilePendingBillingAttempts(agedNow()) }

        // when
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        billingChargeTxService.applyLookup(seeded.chargeId, seeded.tossOrderId, doneRemote("pay-lookup"), due)
        release.countDown()
        thread.join(10_000)

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        assertEquals(TossBillingChargeStatus.DONE, charge.status)
        assertEquals("pay-lookup", charge.paymentKey)
        val subscription = subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow()
        assertEquals(due.plusMonths(1), subscription.nextBillingAt)
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    private fun assertLateDoneDoesNotUndoFailure() {
        // given
        clearMocks(tossPaymentsApiClient, notificationCommandService)
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val seeded = seedPending(due, oneOff = false)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.getPaymentByOrderId(seeded.tossOrderId) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            doneRemote("pay-late")
        }
        val thread = Thread { scheduler.reconcilePendingBillingAttempts(agedNow()) }

        // when
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        billingChargeReconcileTxService.apply(seeded.chargeId, seeded.attemptId, TossApiResponse(404, null, null), due)
        release.countDown()
        thread.join(10_000)

        // then
        val charge = chargeRepository.findById(seeded.chargeId).orElseThrow()
        val subscription = subscriptionRepository.findById(seeded.subscriptionId!!).orElseThrow()
        assertEquals(TossBillingChargeStatus.FAILED, charge.status)
        assertEquals(due, subscription.nextBillingAt)
        assertEquals(1, subscription.failCount)
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    private fun assertStillPending(seeded: SeededAttempt) {
        assertEquals(TossBillingChargeStatus.PENDING, chargeRepository.findById(seeded.chargeId).orElseThrow().status)
        assertEquals(TossBillingChargeStatus.PENDING, attemptRepository.findById(seeded.attemptId).orElseThrow().status)
    }

    private fun seedPending(
        due: LocalDateTime,
        oneOff: Boolean,
    ): SeededAttempt {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val user =
            userJpaRepository.save(
                User(email = "br$token@test.com", nickname = Nickname.of("r$token")),
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
        val subscriptionId = if (oneOff) null else saveSubscription(user.id, method, due)
        val orderId =
            if (oneOff) {
                "billing-${user.id}-$token"
            } else {
                BillingChargeExecutor.periodOrderId(subscriptionId!!, due)
            }
        val charge =
            chargeRepository.save(
                TossBillingCharge(
                    userId = user.id,
                    subscriptionId = subscriptionId,
                    billingMethod = method,
                    orderId = orderId,
                    orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                    amount = BillingDefaults.MONTHLY_AMOUNT,
                    status = TossBillingChargeStatus.PENDING,
                ),
            )
        val tossOrderId = BillingChargeExecutor.attemptOrderId(orderId, 1)
        val attempt =
            attemptRepository.save(
                TossBillingChargeAttempt(
                    chargeId = charge.id,
                    billingMethod = method,
                    attemptNo = 1,
                    tossOrderId = tossOrderId,
                    status = TossBillingChargeStatus.PENDING,
                ),
            )
        return SeededAttempt(subscriptionId, charge.id, attempt.id, tossOrderId, orderId)
    }

    private fun saveAttempt(
        seeded: SeededAttempt,
        attemptNo: Int,
        status: TossBillingChargeStatus,
    ): String {
        val tossOrderId = BillingChargeExecutor.attemptOrderId(seeded.orderId, attemptNo)
        TransactionTemplate(transactionManager).executeWithoutResult {
            val method = chargeRepository.findById(seeded.chargeId).orElseThrow().billingMethod
            attemptRepository.save(
                TossBillingChargeAttempt(
                    chargeId = seeded.chargeId,
                    billingMethod = method,
                    attemptNo = attemptNo,
                    tossOrderId = tossOrderId,
                    status = status,
                ),
            )
        }
        return tossOrderId
    }

    private fun saveSubscription(
        userId: Long,
        method: TossBillingMethod,
        due: LocalDateTime,
    ): Long =
        subscriptionRepository
            .save(
                TossBillingSubscription(
                    userId = userId,
                    billingMethod = method,
                    orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                    amount = BillingDefaults.MONTHLY_AMOUNT,
                    nextBillingAt = due,
                ),
            ).id

    private fun doneRemote(paymentKey: String): TossApiResponse {
        val body =
            """{"status":"DONE","totalAmount":4900,"paymentKey":"$paymentKey","approvedAt":"2026-11-01T00:00:00+09:00"}"""
        return TossApiResponse(200, objectMapper.readTree(body), body)
    }

    private fun statusRemote(
        httpStatus: Int,
        status: String?,
        code: String?,
        totalAmount: Long? = null,
    ): TossApiResponse {
        if (httpStatus == 0 || httpStatus == 404) {
            return TossApiResponse(httpStatus, null, null)
        }
        val fields = mutableListOf<String>()
        if (status != null) {
            fields.add(""""status":"$status"""")
        }
        if (code != null) {
            fields.add(""""code":"$code"""")
        }
        if (totalAmount != null) {
            fields.add(""""totalAmount":$totalAmount""")
            fields.add(""""paymentKey":"pay-x"""")
        }
        val body = "{${fields.joinToString(",")}}"
        return TossApiResponse(httpStatus, objectMapper.readTree(body), body)
    }

    private fun captureLogs(block: () -> Unit): List<String> {
        val appender = ListAppender<ILoggingEvent>()
        val logger = LoggerFactory.getLogger(TossBillingReconcileScheduler::class.java) as Logger
        appender.start()
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.map { it.formattedMessage }
    }

    private fun agedNow(): LocalDateTime = LocalDateTime.now().plusMinutes(2)

    @DisplayName("빌링 조회 스케줄은 인자 없는 메서드만 잠근다")
    @Test
    fun billingReconcileSchedulerUsesShedLock() {
        // given
        val locked = TossBillingReconcileScheduler::class.java.getMethod("reconcilePendingBillingAttempts")
        val direct =
            TossBillingReconcileScheduler::class.java.getMethod(
                "reconcilePendingBillingAttempts",
                LocalDateTime::class.java,
            )

        // when
        val lock = locked.getAnnotation(SchedulerLock::class.java)
        val directLock = direct.getAnnotation(SchedulerLock::class.java)

        // then
        assertEquals("tossBillingReconcile", lock.name)
        assertEquals("PT1M", lock.lockAtLeastFor)
        assertEquals("PT4M", lock.lockAtMostFor)
        assertNull(directLock)
    }

    private data class SeededAttempt(
        val subscriptionId: Long?,
        val chargeId: Long,
        val attemptId: Long,
        val tossOrderId: String,
        val orderId: String,
    )

    private data class RemoteCase(
        val httpStatus: Int,
        val status: String?,
        val code: String?,
        val fails: Boolean,
    )
}
