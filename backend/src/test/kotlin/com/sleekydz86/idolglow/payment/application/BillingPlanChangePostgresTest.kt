package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.BillingPlanChangeExceptionType
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChangeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingPlanChangeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("이용권 즉시 변경 PostgreSQL")
class BillingPlanChangePostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var service: BillingPlanChangeService

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
    private lateinit var planChangeRepository: TossBillingPlanChangeJpaRepository

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()
    private val now = LocalDateTime.of(2026, 11, 28, 10, 0)
    private val due = LocalDateTime.of(2027, 2, 28, 9, 0)
    private val upgrade = PlanChangeCommand(cycleMonths = 12, expectedCardAmount = 33_252L, expectedCreditAdded = 0L)

    @BeforeEach
    fun setUp() {
        clearMocks(tossPaymentsApiClient)
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
    }

    @DisplayName("상향 변경은 차액을 카드로 결제한 뒤 1년권으로 바꾸고 결제 내역을 남긴다")
    @Test
    fun upgradeChargesDifference() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        approveWith(33_252L)

        // when
        val result = service.change(seeded.userId, upgrade, now)

        // then
        val subscription = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        val change = planChangeRepository.findAll().single { it.subscriptionId == seeded.subscriptionId }
        val charge = chargeRepository.findById(change.chargeId!!).orElseThrow()
        assertThat(result.status).isEqualTo(TossBillingPlanChangeStatus.DONE)
        assertThat(subscription.cycleMonths).isEqualTo(12)
        assertThat(subscription.amount).isEqualTo(47_000L)
        assertThat(subscription.nextBillingAt).isEqualTo(now.plusMonths(12))
        assertThat(subscription.lastChargedAt).isEqualTo(now)
        assertThat(change.status).isEqualTo(TossBillingPlanChangeStatus.DONE)
        assertThat(change.remainingValue).isEqualTo(13_748L)
        assertThat(charge.status).isEqualTo(TossBillingChargeStatus.DONE)
        assertThat(charge.amount).isEqualTo(33_252L)
        assertThat(charge.subscriptionId).isNull()
        assertThat(chargeRepository.findAllByUserIdOrderByIdDesc(seeded.userId)).hasSize(1)
    }

    @DisplayName("같은 변경을 다시 보내면 거절하고 카드 결제는 한 번뿐이다")
    @Test
    fun duplicateRequestChargesOnce() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        approveWith(33_252L)
        service.change(seeded.userId, upgrade, now)

        // when
        val second = catchThrowableOfType(CustomException::class.java) { service.change(seeded.userId, upgrade, now) }

        // then
        assertThat(second.getExceptionType()).isEqualTo(BillingPlanChangeExceptionType.SAME_CYCLE)
        verifyCharged(1)
        assertThat(chargeRepository.findAllByUserIdOrderByIdDesc(seeded.userId)).hasSize(1)
    }

    @DisplayName("동시에 두 번 바꾸면 하나만 결제되고 나머지는 결과 대기로 거절한다")
    @Test
    fun concurrentRequestsChargeOnce() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) } answers {
            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            approved(33_252L)
        }
        val pool = Executors.newFixedThreadPool(2)

        // when
        val first = pool.submit<Result<Any>> { runCatching { service.change(seeded.userId, upgrade, now) } }
        entered.await(10, TimeUnit.SECONDS)
        val second = pool.submit<Result<Any>> { runCatching { service.change(seeded.userId, upgrade, now) } }
        val secondResult = second.get(10, TimeUnit.SECONDS)
        release.countDown()
        val firstResult = first.get(10, TimeUnit.SECONDS)
        pool.shutdown()

        // then
        assertThat(firstResult.isSuccess).isTrue()
        val rejected = secondResult.exceptionOrNull() as CustomException
        assertThat(rejected.getExceptionType()).isEqualTo(BillingPlanChangeExceptionType.CHARGE_PENDING)
        verifyCharged(1)
    }

    @DisplayName("차액 결제가 실패하면 구독과 적립금과 실패 횟수가 그대로이고 다시 시도하면 바뀐다")
    @Test
    fun failedPaymentKeepsSubscription() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        stubCharge(rejected())

        // when
        val failure = catchThrowableOfType(CustomException::class.java) { service.change(seeded.userId, upgrade, now) }

        // then
        val afterFailure = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        val change = planChangeRepository.findAll().single { it.subscriptionId == seeded.subscriptionId }
        assertThat(failure.getExceptionType()).isEqualTo(BillingPlanChangeExceptionType.PAYMENT_FAILED)
        assertThat(afterFailure.cycleMonths).isEqualTo(3)
        assertThat(afterFailure.nextBillingAt).isEqualTo(due)
        assertThat(afterFailure.failCount).isZero()
        assertThat(afterFailure.creditAmount).isZero()
        assertThat(change.status).isEqualTo(TossBillingPlanChangeStatus.FAILED)

        approveWith(33_252L)
        val retried = service.change(seeded.userId, upgrade, now)
        assertThat(retried.status).isEqualTo(TossBillingPlanChangeStatus.DONE)
        assertThat(planChangeRepository.findAll().count { it.subscriptionId == seeded.subscriptionId }).isEqualTo(1)
    }

    @DisplayName("결제 결과를 모르면 구독을 바꾸지 않고 조회로 확정되면 그때 바꾼다")
    @Test
    fun pendingResultAppliedAfterLookup() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        stubCharge(TossApiResponse(500, null, null))

        // when
        val pending = service.change(seeded.userId, upgrade, now)
        val beforeLookup = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        val change = planChangeRepository.findAll().single { it.subscriptionId == seeded.subscriptionId }
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns approved(33_252L)
        executor.finalizeCharge(change.chargeId!!, now, null)

        // then
        assertThat(pending.status).isEqualTo(TossBillingPlanChangeStatus.PENDING)
        assertThat(beforeLookup.cycleMonths).isEqualTo(3)
        val afterLookup = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        assertThat(afterLookup.cycleMonths).isEqualTo(12)
        val settled = planChangeRepository.findById(change.id).orElseThrow()
        assertThat(settled.status).isEqualTo(TossBillingPlanChangeStatus.DONE)
    }

    @DisplayName("하향 변경은 카드 결제 없이 남은 돈을 적립하고 바로 바꾼다")
    @Test
    fun downgradeAddsCredit() {
        // given
        val yearDue = LocalDateTime.of(2027, 11, 28, 9, 0)
        val seeded = seedSubscription(cycleMonths = 12, amount = 47_000L, nextBillingAt = yearDue)
        val command = PlanChangeCommand(cycleMonths = 3, expectedCardAmount = 0L, expectedCreditAdded = 32_971L)

        // when
        val result = service.change(seeded.userId, command, now)

        // then
        val subscription = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        assertThat(result.status).isEqualTo(TossBillingPlanChangeStatus.DONE)
        assertThat(subscription.cycleMonths).isEqualTo(3)
        assertThat(subscription.creditAmount).isEqualTo(32_971L)
        assertThat(subscription.lastChargedAt).isEqualTo(LocalDateTime.of(2026, 11, 28, 9, 0))
        verifyCharged(0)
    }

    @DisplayName("화면이 본 금액과 다시 계산한 금액이 다르면 아무것도 남기지 않고 거절한다")
    @Test
    fun rejectsChangedAmount() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L)
        val stale = upgrade.copy(expectedCardAmount = 33_000L)

        // when
        val error = catchThrowableOfType(CustomException::class.java) { service.change(seeded.userId, stale, now) }

        // then
        assertThat(error.getExceptionType()).isEqualTo(BillingPlanChangeExceptionType.AMOUNT_CHANGED)
        assertThat(planChangeRepository.findAll().none { it.subscriptionId == seeded.subscriptionId }).isTrue()
        assertThat(subscriptionRepository.findById(seeded.subscriptionId).orElseThrow().cycleMonths).isEqualTo(3)
    }

    @DisplayName("정기결제는 적립금을 먼저 빼고 나머지만 카드로 결제한 뒤 적립금을 줄인다")
    @Test
    fun periodChargeUsesCredit() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L, nextBillingAt = now, credit = 5_000L)
        approveWith(8_900L)

        // when
        val charge = executor.chargeSubscription(seeded.subscriptionId, now)

        // then
        val subscription = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        assertThat(charge.status).isEqualTo(TossBillingChargeStatus.DONE)
        assertThat(charge.amount).isEqualTo(8_900L)
        assertThat(charge.creditUsed).isEqualTo(5_000L)
        assertThat(subscription.creditAmount).isZero()
        assertThat(subscription.nextBillingAt).isEqualTo(now.plusMonths(3))
    }

    @DisplayName("적립금이 이용권 금액 이상이면 카드 결제 없이 회차를 내고 적립금만 줄인다")
    @Test
    fun periodChargeCoveredByCredit() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L, nextBillingAt = now, credit = 20_000L)

        // when
        val charge = executor.chargeSubscription(seeded.subscriptionId, now)

        // then
        val subscription = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        assertThat(charge.status).isEqualTo(TossBillingChargeStatus.DONE)
        assertThat(charge.amount).isZero()
        assertThat(subscription.creditAmount).isEqualTo(6_100L)
        assertThat(subscription.nextBillingAt).isEqualTo(now.plusMonths(3))
        verifyCharged(0)
    }

    @DisplayName("정기결제가 실패하면 적립금은 줄지 않는다")
    @Test
    fun failedPeriodChargeKeepsCredit() {
        // given
        val seeded = seedSubscription(cycleMonths = 3, amount = 13_900L, nextBillingAt = now, credit = 5_000L)
        stubCharge(rejected())

        // when
        runCatching { executor.chargeSubscription(seeded.subscriptionId, now) }

        // then
        val subscription = subscriptionRepository.findById(seeded.subscriptionId).orElseThrow()
        assertThat(subscription.creditAmount).isEqualTo(5_000L)
        assertThat(subscription.failCount).isEqualTo(1)
    }

    private fun stubCharge(response: TossApiResponse) {
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns response
    }

    private fun verifyCharged(times: Int) {
        verify(exactly = times) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun approveWith(amount: Long) {
        stubCharge(approved(amount))
    }

    private fun approved(amount: Long): TossApiResponse {
        val raw = """{"paymentKey":"pk-${UUID.randomUUID()}","status":"DONE","totalAmount":$amount}"""
        return TossApiResponse(200, objectMapper.readTree(raw), raw)
    }

    private fun rejected(): TossApiResponse {
        val raw = """{"message":"카드사 거절"}"""
        return TossApiResponse(400, objectMapper.readTree(raw), raw)
    }

    private fun seedSubscription(
        cycleMonths: Int,
        amount: Long,
        nextBillingAt: LocalDateTime = due,
        credit: Long = 0L,
    ): Seeded {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val user = userJpaRepository.save(User(email = "pc$token@test.com", nickname = Nickname.of("p$token")))
        val method =
            methodRepository.save(
                TossBillingMethod(
                    userId = user.id,
                    customerKey = "idolglow-user-${user.id}",
                    billingKey = "bk-$token",
                    isDefault = true,
                ),
            )
        val subscription =
            subscriptionRepository.save(
                TossBillingSubscription(
                    userId = user.id,
                    billingMethod = method,
                    orderName = "IdolGlow Glow Pass ${cycleMonths}개월",
                    amount = amount,
                    nextBillingAt = nextBillingAt,
                    lastChargedAt = LocalDateTime.of(2026, 11, 28, 9, 0),
                    cycleMonths = cycleMonths,
                    creditAmount = credit,
                ),
            )
        return Seeded(user.id, subscription.id)
    }

    private data class Seeded(
        val userId: Long,
        val subscriptionId: Long,
    )
}
