package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.application.dto.IssueTossBillingKeyRequest
import com.sleekydz86.idolglow.payment.domain.TossBillingCustomer
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingCustomerJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingDueClaimRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

@DisplayName("토스 빌링 구매자 키")
class TossBillingCustomerKeyTest {
    private val customerRepository = mockk<TossBillingCustomerJpaRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>(relaxed = true)
    private val stored = mutableMapOf<Long, TossBillingCustomer>()
    private lateinit var service: TossBillingService

    @BeforeEach
    fun setUp() {
        stored.clear()
        every { customerRepository.findByUserId(any()) } answers { stored[firstArg()] }
        every { customerRepository.saveAndFlush(any()) } answers {
            val row = firstArg<TossBillingCustomer>()
            val previous = stored.putIfAbsent(row.userId, row)
            if (previous != null) {
                throw DataIntegrityViolationException("user_id 중복 userId=${row.userId}")
            }
            row
        }
        val self = mockk<TossBillingService>()
        service =
            TossBillingService(
                tossPaymentProperties =
                    TossPaymentProperties(
                        enabled = true,
                        secretKey = "test-secret",
                        clientKey = "test-client",
                    ),
                tossPaymentsApiClient = tossPaymentsApiClient,
                methodRepository = mockk(relaxed = true),
                customerRepository = customerRepository,
                subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>(relaxed = true),
                chargeRepository = mockk<TossBillingChargeJpaRepository>(relaxed = true),
                attemptRepository = mockk<TossBillingChargeAttemptJpaRepository>(relaxed = true),
                dueClaimRepository = mockk<TossBillingDueClaimRepository>(relaxed = true),
                billingChargeExecutor = mockk(relaxed = true),
                self = self,
                frontendBaseUrl = "http://localhost:3000",
                defaultAmount = BillingDefaults.MONTHLY_AMOUNT,
                defaultOrderName = BillingDefaults.DEFAULT_ORDER_NAME,
            )
        every { self.insertCustomerKey(any()) } answers { service.insertCustomerKey(firstArg()) }
    }

    @DisplayName("같은 사용자는 같은 UUID를 받고 사용자 아이디는 키에 없다")
    @Test
    fun customerKeyIsRandomAndStable() {
        // given

        // when
        val first = service.clientConfig(USER_A).customerKey
        val second = service.clientConfig(USER_A).customerKey
        val other = service.clientConfig(USER_B).customerKey

        // then
        assertThat(first).isEqualTo(second)
        assertThat(UUID.fromString(first).toString()).isEqualTo(first)
        assertThat(first).doesNotContain(USER_A.toString())
        assertThat(other).isNotEqualTo(first)
        assertThat(other).doesNotContain(USER_B.toString())
    }

    @DisplayName("예전 형식 customerKey로 빌링키 발급을 요청하면 거절한다")
    @Test
    fun billingKeyIssueRejectsOldFormatKey() {
        // given
        service.clientConfig(USER_A)

        // when
        val thrown =
            assertThatThrownBy {
                service.persistBillingKey(
                    USER_A,
                    IssueTossBillingKeyRequest(
                        authKey = "auth-key",
                        customerKey = "idolglow-user-$USER_A",
                    ),
                )
            }

        // then
        thrown
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("customerKey가 일치하지 않습니다.")
        verify(exactly = 0) { tossPaymentsApiClient.issueBillingKey(any(), any()) }
    }

    @DisplayName("user_id가 겹치면 이미 저장된 customerKey를 돌려준다")
    @Test
    fun customerKeyUniqueConflictReturnsStored() {
        // given
        val winner = UUID.randomUUID().toString()
        var lookups = 0
        every { customerRepository.findByUserId(USER_A) } answers {
            lookups += 1
            if (lookups >= 3) {
                TossBillingCustomer(userId = USER_A, customerKey = winner)
            } else {
                null
            }
        }
        every { customerRepository.saveAndFlush(any()) } throws
            DataIntegrityViolationException("uk_toss_billing_customers_user_id")

        // when
        val key = service.clientConfig(USER_A).customerKey

        // then
        assertThat(key).isEqualTo(winner)
    }

    private companion object {
        const val USER_A = 1_234_567_890_123L
        const val USER_B = 9_876_543_210_987L
    }
}
