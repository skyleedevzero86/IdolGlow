package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.application.dto.ChargeTossBillingRequest
import com.sleekydz86.idolglow.payment.application.dto.IssueTossBillingKeyRequest
import com.sleekydz86.idolglow.payment.application.dto.StartTossBillingSubscriptionRequest
import com.sleekydz86.idolglow.payment.application.dto.TossBillingChargeResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingClientConfigResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingMethodResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingSubscriptionResponse
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingDueClaimRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
class TossBillingService(
    private val tossPaymentProperties: TossPaymentProperties,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val methodRepository: TossBillingMethodJpaRepository,
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val dueClaimRepository: TossBillingDueClaimRepository,
    private val billingChargeExecutor: BillingChargeExecutor,
    @Lazy private val self: TossBillingService,
    @param:Value("\${app.frontend-base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
    @param:Value("\${payment.toss.billing.default-amount:4900}")
    private val defaultAmount: Long,
    @param:Value("\${payment.toss.billing.default-order-name:IdolGlow 월간 구독}")
    private val defaultOrderName: String,
) {
    fun clientConfig(userId: Long): TossBillingClientConfigResponse {
        require(tossPaymentProperties.clientKey.isNotBlank()) { "payment.toss.client-key 가 비어 있습니다." }
        val base = frontendBaseUrl.trim().trimEnd('/')
        return TossBillingClientConfigResponse(
            clientKey = tossPaymentProperties.clientKey.trim(),
            customerKey = customerKeyOf(userId),
            successUrl = "$base/billing/success",
            failUrl = "$base/billing/fail",
        )
    }

    @Transactional(readOnly = true)
    fun overview(userId: Long): TossBillingOverviewResponse {
        val method = methodRepository.findByUserIdAndActiveTrue(userId)
        val subscription =
            subscriptionRepository
                .findByUserIdAndPlanCode(userId, PLAN_MONTHLY)
                ?.also { it.finalizeCanceledIfEnded() }
                ?.takeIf { it.status != TossBillingSubscriptionStatus.CANCELED }
        val charges = chargeRepository.findAllByUserIdOrderByIdDesc(userId).take(20)
        return TossBillingOverviewResponse(
            method = method?.let(TossBillingMethodResponse::from),
            subscription = subscription?.let(TossBillingSubscriptionResponse::from),
            charges = charges.map(TossBillingChargeResponse::from),
        )
    }

    fun issueBillingKey(
        userId: Long,
        request: IssueTossBillingKeyRequest,
    ): TossBillingMethodResponse {
        val saved = self.persistBillingKey(userId, request)
        val resumedId = self.resumePausedSubscription(userId, saved.id)
        if (resumedId != null) {
            runCatching { billingChargeExecutor.chargeSubscription(resumedId) }
        }
        return saved
    }

    @Transactional
    fun persistBillingKey(
        userId: Long,
        request: IssueTossBillingKeyRequest,
    ): TossBillingMethodResponse {
        requireEnabled()
        val expectedCustomerKey = customerKeyOf(userId)
        require(request.customerKey == expectedCustomerKey) { "customerKey가 일치하지 않습니다." }

        val response =
            tossPaymentsApiClient.issueBillingKey(
                authKey = request.authKey.trim(),
                customerKey = request.customerKey.trim(),
            )
        if (!response.isSuccess2xx || response.json == null) {
            val message = response.json?.path("message")?.asText(null) ?: response.rawBody ?: "빌링키 발급에 실패했습니다."
            throw IllegalStateException(message)
        }

        val json = response.json
        val billingKey =
            json.path("billingKey").asText(null)?.trim()
                ?: throw IllegalStateException("빌링키 응답이 비어 있습니다.")
        val card = json.path("card")
        methodRepository.findByUserIdAndActiveTrue(userId)?.deactivate()

        val saved =
            methodRepository.save(
                TossBillingMethod(
                    userId = userId,
                    customerKey = expectedCustomerKey,
                    billingKey = billingKey,
                    cardCompany = json.path("cardCompany").asText(null) ?: card.path("company").asText(null),
                    cardNumber = json.path("cardNumber").asText(null) ?: card.path("number").asText(null),
                    cardType = card.path("cardType").asText(null),
                    ownerType = card.path("ownerType").asText(null),
                    authenticatedAt = parseDateTime(json.path("authenticatedAt").asText(null)),
                    active = true,
                ),
            )
        return TossBillingMethodResponse.from(saved)
    }

    @Transactional
    fun resumePausedSubscription(
        userId: Long,
        methodId: Long,
    ): Long? {
        val paused = subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY) ?: return null
        if (paused.status != TossBillingSubscriptionStatus.PAUSED) {
            paused.billingMethod = methodRepository.findById(methodId).orElse(paused.billingMethod)
            return null
        }
        val method =
            methodRepository.findById(methodId).orElseThrow {
                IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
            }
        paused.billingMethod = method
        paused.status = TossBillingSubscriptionStatus.ACTIVE
        paused.failCount = 0
        paused.retryAt = null
        paused.nextBillingAt = LocalDateTime.now()
        return paused.id
    }

    fun startSubscription(
        userId: Long,
        request: StartTossBillingSubscriptionRequest,
    ): TossBillingSubscriptionResponse {
        val subscriptionId = self.saveSubscription(userId, request)
        if (request.chargeImmediately) {
            billingChargeExecutor.chargeSubscription(subscriptionId)
        }
        return self.subscriptionResponse(subscriptionId)
    }

    @Transactional
    fun saveSubscription(
        userId: Long,
        request: StartTossBillingSubscriptionRequest,
    ): Long {
        requireEnabled()
        val method =
            methodRepository.findByUserIdAndActiveTrue(userId)
                ?: throw IllegalArgumentException("등록된 결제 수단이 없습니다. 먼저 카드를 등록하세요.")

        val amount = if (request.amount > 0) request.amount else defaultAmount
        val orderName = request.orderName.ifBlank { defaultOrderName }
        val now = LocalDateTime.now()
        val existing = subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY)
        val subscription =
            if (existing == null) {
                TossBillingSubscription(
                    userId = userId,
                    billingMethod = method,
                    planCode = PLAN_MONTHLY,
                    orderName = orderName,
                    amount = amount,
                    status = TossBillingSubscriptionStatus.ACTIVE,
                    nextBillingAt = if (request.chargeImmediately) now else now.plusMonths(1),
                )
            } else {
                existing.billingMethod = method
                existing.orderName = orderName
                existing.amount = amount
                existing.status = TossBillingSubscriptionStatus.ACTIVE
                existing.failCount = 0
                existing.retryAt = null
                existing.endsAt = null
                if (request.chargeImmediately) {
                    existing.nextBillingAt = now
                } else if (existing.nextBillingAt == null) {
                    existing.nextBillingAt = now.plusMonths(1)
                }
                existing
            }
        return subscriptionRepository.saveAndFlush(subscription).id
    }

    @Transactional(readOnly = true)
    fun subscriptionResponse(subscriptionId: Long): TossBillingSubscriptionResponse {
        val subscription =
            subscriptionRepository.findWithBillingMethodById(subscriptionId)
                ?: throw IllegalArgumentException("구독을 찾을 수 없습니다. subscriptionId=$subscriptionId")
        return TossBillingSubscriptionResponse.from(subscription)
    }

    @Transactional
    fun cancelSubscription(userId: Long): TossBillingSubscriptionResponse {
        val subscription =
            subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY)
                ?: throw IllegalArgumentException("구독 정보가 없습니다.")
        subscription.scheduleCancel()
        return TossBillingSubscriptionResponse.from(subscription)
    }

    @Transactional
    fun withdrawCancelSubscription(userId: Long): TossBillingSubscriptionResponse {
        val subscription =
            subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY)
                ?: throw IllegalArgumentException("구독 정보가 없습니다.")
        subscription.withdrawCancel()
        return TossBillingSubscriptionResponse.from(subscription)
    }

    fun chargeNow(
        userId: Long,
        request: ChargeTossBillingRequest,
    ): TossBillingChargeResponse {
        val context = self.loadOneOffChargeContext(userId, request)
        val charge =
            billingChargeExecutor.chargeOneOff(
                userId = userId,
                methodId = context.method.id,
                amount = context.amount,
                orderName = context.orderName,
            )
        return TossBillingChargeResponse.from(charge)
    }

    @Transactional(readOnly = true)
    fun loadOneOffChargeContext(
        userId: Long,
        request: ChargeTossBillingRequest,
    ): OneOffChargeContext {
        requireEnabled()
        val method =
            methodRepository.findByUserIdAndActiveTrue(userId)
                ?: throw IllegalArgumentException("등록된 결제 수단이 없습니다.")
        val subscription = subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY)
        val amount = if (request.amount > 0) request.amount else (subscription?.amount ?: defaultAmount)
        val orderName = request.orderName.ifBlank { subscription?.orderName ?: defaultOrderName }
        return OneOffChargeContext(method = method, amount = amount, orderName = orderName)
    }

    data class OneOffChargeContext(
        val method: TossBillingMethod,
        val amount: Long,
        val orderName: String,
    )

    @Transactional
    fun findDueSubscriptionIds(now: LocalDateTime = LocalDateTime.now()): List<Long> {
        if (!tossPaymentProperties.enabled) {
            return emptyList()
        }
        return dueClaimRepository.claimDueSubscriptionIds(now)
    }

    fun chargeOne(subscriptionId: Long): Boolean =
        runCatching {
            billingChargeExecutor.chargeSubscription(subscriptionId)
            true
        }.getOrDefault(false)

    private fun requireEnabled() {
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다. payment.toss.enabled=true 로 설정하세요." }
        require(tossPaymentProperties.secretKey.isNotBlank()) { "payment.toss.secret-key 가 비어 있습니다." }
        require(tossPaymentProperties.clientKey.isNotBlank()) { "payment.toss.client-key 가 비어 있습니다." }
    }

    private fun customerKeyOf(userId: Long): String = "idolglow-user-$userId"

    private fun parseDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }

    companion object {
        private const val PLAN_MONTHLY = "MONTHLY"
    }
}
