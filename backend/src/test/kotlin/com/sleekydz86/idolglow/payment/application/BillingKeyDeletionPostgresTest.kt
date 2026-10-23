package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.dto.IssueTossBillingKeyRequest
import com.sleekydz86.idolglow.payment.domain.TossBillingCustomer
import com.sleekydz86.idolglow.payment.domain.TossBillingKeyDeletionStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.BillingKeyCrypto
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingCustomerJpaRepository
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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

@DisplayName("빌링키 삭제 PostgreSQL")
@Suppress("TooManyFunctions")
class BillingKeyDeletionPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var billingService: TossBillingService

    @Autowired
    private lateinit var webhookService: TossWebhookService

    @Autowired
    private lateinit var scheduler: TossBillingKeyDeletionScheduler

    @Autowired
    private lateinit var methodRepository: TossBillingMethodJpaRepository

    @Autowired
    private lateinit var subscriptionRepository: TossBillingSubscriptionJpaRepository

    @Autowired
    private lateinit var customerRepository: TossBillingCustomerJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean(relaxed = true)
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    @MockkBean(relaxed = true)
    private lateinit var notificationCommandService: NotificationCommandService

    @BeforeEach
    fun setUp() {
        clearMocks(tossPaymentsApiClient, notificationCommandService)
    }

    @DisplayName("카드 삭제는 커밋 뒤 토스 DELETE를 호출하고 잠금을 쥐지 않는다")
    @Test
    fun deactivateMethodDeletesTossBillingKey() {
        // given
        val seeded = seed(billingKey = "bk-lock", withSubscription = false)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            TossApiResponse(200, null, "")
        }
        val thread = Thread { billingService.deactivateMethod(seeded.userId, seeded.methodId) }

        // when
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        val started = System.nanoTime()
        TransactionTemplate(transactionManager).executeWithoutResult {
            methodRepository.findByIdForUpdate(seeded.methodId)
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        release.countDown()
        thread.join(10_000)

        // then
        assertTrue(elapsedMs < 1_000, "elapsedMs=$elapsedMs")
        assertDeleted(seeded)
    }

    @DisplayName("삭제 응답이 없으면 대기 상태로 남고 결제에 쓰이지 않는다")
    @Test
    fun billingDeletionTimeoutStaysPending() {
        // given
        val seeded = seed(billingKey = "bk-timeout", withSubscription = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(0, null, null)

        // when
        billingService.deactivateMethod(seeded.userId, seeded.methodId)

        // then
        assertPending(seeded)
        assertTrue(methodRepository.findAllByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(seeded.userId).isEmpty())
    }

    @DisplayName("삭제 5xx는 대기 상태로 남아 재시도 대상이다")
    @Test
    fun billingDeletionFiveXxStaysPending() {
        // given
        val seeded = seed(billingKey = "bk-500", withSubscription = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(500, null, null)

        // when
        billingService.deactivateMethod(seeded.userId, seeded.methodId)

        // then
        val method = assertPending(seeded)
        assertEquals(1, method.deletionAttemptCount)
        assertTrue(retryIds(LocalDateTime.now().plusMinutes(2)).contains(seeded.methodId))
    }

    @DisplayName("삭제 404는 이미 삭제된 것으로 확정한다")
    @Test
    fun billingDeletionNotFoundIsDeleted() {
        // given
        val seeded = seed(billingKey = "bk-404", withSubscription = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(404, null, null)

        // when
        billingService.deactivateMethod(seeded.userId, seeded.methodId)

        // then
        assertDeleted(seeded)
    }

    @DisplayName("삭제 확정 4xx는 실패로 남기고 다시 활성화하지 않는다")
    @Test
    fun billingDeletionFourXxNeedsReview() {
        // given
        val seeded = seed(billingKey = "bk-400", withSubscription = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(500, null, null)
        every { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) } returns TossApiResponse(400, null, null)

        // when
        billingService.deactivateMethod(seeded.userId, seeded.methodId)
        scheduler.retryPendingDeletions(LocalDateTime.now().plusMinutes(2))

        // then
        val method = methodRepository.findById(seeded.methodId).orElseThrow()
        assertEquals(TossBillingKeyDeletionStatus.DELETE_FAILED, method.deletionStatus)
        assertFalse(method.active)
        assertFalse(method.usableForCharge())
        verify(exactly = 1) { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) }
    }

    @DisplayName("삭제 재시도는 같은 빌링키만 다시 지운다")
    @Test
    fun billingDeletionRetryUsesSameBillingKey() {
        // given
        val seeded = seed(billingKey = "bk-same", withSubscription = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(500, null, null)
        every { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) } returns TossApiResponse(500, null, null)
        billingService.deactivateMethod(seeded.userId, seeded.methodId)
        every { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) } returns TossApiResponse(200, null, "")

        // when
        scheduler.retryPendingDeletions(LocalDateTime.now().plusMinutes(2))

        // then
        verify(exactly = 2) { tossPaymentsApiClient.deleteBillingKey(seeded.billingKey) }
        assertEquals(1, methodRepository.countByUserId(seeded.userId))
        assertDeleted(seeded)
    }

    @DisplayName("웹훅이 먼저 삭제 완료하면 늦은 거절 응답이 덮지 않는다")
    @Test
    fun billingDeletionDoesNotOverwriteWebhookCompletion() {
        // given
        val seeded = seed(billingKey = "bk-late", withSubscription = false)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            TossApiResponse(400, null, null)
        }
        val thread = Thread { billingService.deactivateMethod(seeded.userId, seeded.methodId) }

        // when
        thread.start()
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        webhookService.handlePayload(deletedBody(seeded.billingKey, seeded.customerKey))
        release.countDown()
        thread.join(10_000)

        // then
        assertDeleted(seeded)
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("BILLING_DELETED 웹훅은 해시가 같은 카드만 비활성화한다")
    @Test
    fun billingDeletedWebhookDeactivatesMethod() {
        // given
        val seeded = seed(billingKey = "bk-hook", withSubscription = false)

        // when
        webhookService.handlePayload(deletedBody(seeded.billingKey, seeded.customerKey))

        // then
        assertDeleted(seeded)
    }

    @DisplayName("같은 삭제 웹훅을 다시 받아도 알림은 한 번이다")
    @Test
    fun duplicateBillingDeletedWebhookIsIdempotent() {
        // given
        val seeded = seed(billingKey = "bk-dup", withSubscription = true)
        val body = deletedBody(seeded.billingKey, seeded.customerKey)

        // when
        webhookService.handlePayload(body)
        webhookService.handlePayload(body)

        // then
        assertDeleted(seeded)
        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription(seeded).status)
        verify(exactly = 1) {
            notificationCommandService.create(
                seeded.userId,
                NotificationType.PAYMENT_FAILED,
                "결제 수단을 다시 등록해 주세요",
                "등록된 결제 수단이 삭제되어 다음 자동결제를 진행할 수 없습니다.",
                "/billing",
            )
        }
    }

    @DisplayName("customerKey만 같으면 다른 카드를 끄지 않는다")
    @Test
    fun billingDeletedWebhookDoesNotTrustCustomerKeyAlone() {
        // given
        val keep = seed(billingKey = "bk-keep", withSubscription = false)
        val drop = addCard(keep, billingKey = "bk-drop", defaultCard = false)

        // when
        webhookService.handlePayload(deletedBody(drop.billingKey, keep.customerKey))

        // then
        val kept = methodRepository.findById(keep.methodId).orElseThrow()
        assertEquals(TossBillingKeyDeletionStatus.ACTIVE, kept.deletionStatus)
        assertTrue(kept.active)
        assertDeleted(drop)
    }

    @DisplayName("기본 카드를 지우면 다른 활성 카드로 구독을 옮긴다")
    @Test
    fun deletedDefaultMethodSelectsNextActiveMethod() {
        // given
        val first = seed(billingKey = "bk-first", withSubscription = true)
        val next = addCard(first, billingKey = "bk-next", defaultCard = false)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(200, null, "")

        // when
        billingService.deactivateMethod(first.userId, first.methodId)

        // then
        val replacement = methodRepository.findById(next.methodId).orElseThrow()
        val subscription = subscription(first)
        assertTrue(replacement.isDefault)
        assertEquals(next.methodId, subscription.billingMethod.id)
        assertEquals(TossBillingSubscriptionStatus.ACTIVE, subscription.status)
        verify(exactly = 0) {
            notificationCommandService.create(any(), any(), any(), any(), any())
        }
    }

    @DisplayName("마지막 카드를 지우면 구독을 멈추고 재등록 알림을 한 번 보낸다")
    @Test
    fun deletedOnlyMethodPausesSubscriptionAndNotifiesUser() {
        // given
        val seeded = seed(billingKey = "bk-only", withSubscription = true)
        every { tossPaymentsApiClient.deleteBillingKey(any()) } returns TossApiResponse(200, null, "")

        // when
        billingService.deactivateMethod(seeded.userId, seeded.methodId)

        // then
        val subscription = subscription(seeded)
        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription.status)
        assertNull(subscription.retryAt)
        assertDeleted(seeded)
        verify(exactly = 1) {
            notificationCommandService.create(
                seeded.userId,
                NotificationType.PAYMENT_FAILED,
                "결제 수단을 다시 등록해 주세요",
                "등록된 결제 수단이 삭제되어 다음 자동결제를 진행할 수 없습니다.",
                "/billing",
            )
        }
    }

    @DisplayName("카드 재등록은 삭제 대기·완료·실패 행을 다시 켜지 않는다")
    @Test
    fun persistBillingKeyDoesNotReactivateDeletionRows() {
        // given
        val seeded = seed(billingKey = "bk-old", withSubscription = false)
        val pending = addCard(seeded, billingKey = "bk-pending", defaultCard = false)
        val failed = addCard(seeded, billingKey = "bk-failed", defaultCard = false)
        mark(seeded.methodId) { it.concealDeletedKey(LocalDateTime.now()) }
        mark(pending.methodId) { it.beginDeletion(LocalDateTime.now()) }
        mark(failed.methodId) { it.markDeletionFailed("거절", LocalDateTime.now()) }
        val body = """{"billingKey":"bk-fresh","card":{"number":"4330"}}"""
        every { tossPaymentsApiClient.issueBillingKey(any(), any()) } returns
            TossApiResponse(200, objectMapper.readTree(body), body)

        // when
        billingService.persistBillingKey(
            seeded.userId,
            IssueTossBillingKeyRequest(authKey = "auth", customerKey = seeded.customerKey),
        )

        // then
        assertEquals(TossBillingKeyDeletionStatus.DELETED, reload(seeded.methodId).deletionStatus)
        assertEquals(TossBillingKeyDeletionStatus.DELETE_PENDING, reload(pending.methodId).deletionStatus)
        assertEquals(TossBillingKeyDeletionStatus.DELETE_FAILED, reload(failed.methodId).deletionStatus)
        assertFalse(reload(seeded.methodId).active)
        val activeKeys =
            methodRepository
                .findAllByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(seeded.userId)
                .map { it.billingKey }
        assertTrue(activeKeys.contains("bk-fresh"))
    }

    @DisplayName("빌링키 삭제 스케줄은 인자 없는 메서드만 잠근다")
    @Test
    fun billingDeletionSchedulerUsesShedLock() {
        // given
        val locked = TossBillingKeyDeletionScheduler::class.java.getMethod("retryPendingDeletions")
        val direct =
            TossBillingKeyDeletionScheduler::class.java.getMethod(
                "retryPendingDeletions",
                LocalDateTime::class.java,
            )

        // when
        val lock = locked.getAnnotation(SchedulerLock::class.java)

        // then
        assertEquals("tossBillingKeyDeletion", lock.name)
        assertEquals("PT1M", lock.lockAtLeastFor)
        assertEquals("PT4M", lock.lockAtMostFor)
        assertNull(direct.getAnnotation(SchedulerLock::class.java))
    }

    private fun seed(
        billingKey: String,
        withSubscription: Boolean,
    ): SeededCard {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val user =
            userJpaRepository.save(
                User(email = "del$token@test.com", nickname = Nickname.of("d$token")),
            )
        val customerKey = UUID.randomUUID().toString()
        customerRepository.save(TossBillingCustomer(userId = user.id, customerKey = customerKey))
        val method = saveMethod(user.id, customerKey, billingKey, defaultCard = true)
        val subscriptionId =
            if (withSubscription) {
                subscriptionRepository
                    .save(
                        TossBillingSubscription(
                            userId = user.id,
                            billingMethod = method,
                            planCode = "MONTHLY",
                            orderName = "Glow Pass",
                            amount = 4_900,
                            status = TossBillingSubscriptionStatus.ACTIVE,
                            nextBillingAt = LocalDateTime.now().plusMonths(1),
                        ),
                    ).id
            } else {
                null
            }
        return SeededCard(
            userId = user.id,
            methodId = method.id,
            billingKey = billingKey,
            customerKey = customerKey,
            billingKeyHash = BillingKeyCrypto.sha256Hex(billingKey),
            subscriptionId = subscriptionId,
        )
    }

    private fun addCard(
        owner: SeededCard,
        billingKey: String,
        defaultCard: Boolean,
    ): SeededCard {
        val method = saveMethod(owner.userId, owner.customerKey, billingKey, defaultCard)
        return owner.copy(
            methodId = method.id,
            billingKey = billingKey,
            billingKeyHash = BillingKeyCrypto.sha256Hex(billingKey),
        )
    }

    private fun saveMethod(
        userId: Long,
        customerKey: String,
        billingKey: String,
        defaultCard: Boolean,
    ): TossBillingMethod =
        methodRepository.save(
            TossBillingMethod(
                userId = userId,
                customerKey = customerKey,
                billingKey = billingKey,
                active = true,
                isDefault = defaultCard,
            ),
        )

    private fun mark(
        methodId: Long,
        change: (TossBillingMethod) -> Unit,
    ) {
        val method = methodRepository.findById(methodId).orElseThrow()
        change(method)
        methodRepository.save(method)
    }

    private fun assertDeleted(seeded: SeededCard) {
        val method = reload(seeded.methodId)
        assertEquals(TossBillingKeyDeletionStatus.DELETED, method.deletionStatus)
        assertFalse(method.active)
        assertFalse(method.isDefault)
        assertEquals(TossBillingMethod.REMOVED_BILLING_KEY, method.billingKey)
        assertEquals(seeded.billingKeyHash, method.billingKeyHash)
    }

    private fun assertPending(seeded: SeededCard): TossBillingMethod {
        val method = reload(seeded.methodId)
        assertEquals(TossBillingKeyDeletionStatus.DELETE_PENDING, method.deletionStatus)
        assertFalse(method.active)
        assertEquals(seeded.billingKey, method.billingKey)
        return method
    }

    private fun reload(methodId: Long): TossBillingMethod = methodRepository.findById(methodId).orElseThrow()

    private fun subscription(seeded: SeededCard): TossBillingSubscription {
        val found =
            TransactionTemplate(transactionManager).execute {
                val subscription = subscriptionRepository.findWithBillingMethodById(seeded.subscriptionId!!)
                subscription?.billingMethod?.id
                subscription
            }
        return requireNotNull(found)
    }

    private fun retryIds(now: LocalDateTime): List<Long> =
        methodRepository.findDeletionRetryIds(
            TossBillingKeyDeletionStatus.DELETE_PENDING,
            now.minusMinutes(1),
            org.springframework.data.domain.PageRequest.of(0, 100),
        )

    private fun deletedBody(
        billingKey: String,
        customerKey: String,
    ): String = """{"eventType":"BILLING_DELETED","data":{"billingKey":"$billingKey","customerKey":"$customerKey"}}"""

    private data class SeededCard(
        val userId: Long,
        val methodId: Long,
        val billingKey: String,
        val customerKey: String,
        val billingKeyHash: String,
        val subscriptionId: Long?,
    )
}
