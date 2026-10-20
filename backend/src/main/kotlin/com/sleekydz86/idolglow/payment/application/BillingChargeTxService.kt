package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BillingChargeTxService(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val attemptRepository: TossBillingChargeAttemptJpaRepository,
    private val methodRepository: TossBillingMethodJpaRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun preparePeriodCharge(subscriptionId: Long): PreparedBillingCharge {
        val subscription =
            subscriptionRepository.findWithBillingMethodById(subscriptionId)
                ?: throw IllegalArgumentException("구독을 찾을 수 없습니다. subscriptionId=$subscriptionId")
        require(subscription.status == TossBillingSubscriptionStatus.ACTIVE) {
            "ACTIVE 구독만 자동결제할 수 있습니다. status=${subscription.status}"
        }
        val dueAt =
            subscription.nextBillingAt
                ?: throw IllegalStateException("nextBillingAt 이 비어 있습니다. subscriptionId=$subscriptionId")
        val orderId = BillingChargeExecutor.periodOrderId(subscription.id, dueAt)
        val existing = chargeRepository.findByOrderId(orderId)
        return when {
            existing == null ->
                PreparedBillingCharge(
                    charge =
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
                        ),
                    alreadyDone = false,
                )
            existing.status == TossBillingChargeStatus.DONE ->
                PreparedBillingCharge(charge = existing, alreadyDone = true)
            else -> {
                if (existing.status == TossBillingChargeStatus.FAILED) {
                    existing.clearFailureForRetry()
                    existing.billingMethod = subscription.billingMethod
                }
                PreparedBillingCharge(charge = existing, alreadyDone = false)
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createOneOffPending(
        userId: Long,
        methodId: Long,
        amount: Long,
        orderName: String,
    ): TossBillingCharge {
        val method =
            methodRepository.findById(methodId).orElseThrow {
                IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
            }
        val suffix =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(BillingDefaults.ONE_OFF_ORDER_ID_SUFFIX_LENGTH)
        val orderId = "billing-$userId-$suffix"
        return chargeRepository.save(
            TossBillingCharge(
                userId = userId,
                subscriptionId = null,
                billingMethod = method,
                orderId = orderId,
                orderName = orderName,
                amount = amount,
                status = TossBillingChargeStatus.PENDING,
            ),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepareRetry(
        chargeId: Long,
        method: TossBillingMethod,
    ) {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        require(charge.status == TossBillingChargeStatus.FAILED) {
            "실패한 회차만 다른 카드로 재결제할 수 있습니다. status=${charge.status}"
        }
        charge.clearFailureForRetry()
        charge.billingMethod = method
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun finalizeWithToss(
        chargeId: Long,
        now: LocalDateTime,
        methodOverride: TossBillingMethod? = null,
    ): BillingChargeFinalizeResult {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        val alreadyDone = charge.status == TossBillingChargeStatus.DONE
        val reconciled =
            if (!alreadyDone && charge.status == TossBillingChargeStatus.PENDING) {
                reconcilePendingIfPaid(charge, now)
            } else {
                null
            }
        return when {
            alreadyDone -> BillingChargeFinalizeResult(charge = charge)
            reconciled != null -> BillingChargeFinalizeResult(charge = reconciled)
            else -> approveWithToss(charge, now, methodOverride)
        }
    }

    private fun approveWithToss(
        charge: TossBillingCharge,
        now: LocalDateTime,
        methodOverride: TossBillingMethod?,
    ): BillingChargeFinalizeResult {
        val method = methodOverride ?: charge.billingMethod
        charge.billingMethod = method
        val attempt = savePendingAttempt(charge, method)
        val user =
            userRepository.findById(charge.userId)
                ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다. userId=${charge.userId}")
        val response =
            tossPaymentsApiClient.chargeBilling(
                billingKey = method.billingKey,
                customerKey = method.customerKey,
                amount = charge.amount,
                orderId = attempt.tossOrderId,
                orderName = charge.orderName,
                customerEmail = user.email,
                customerName = user.nickname.value,
                idempotencyKey = "billing-charge-${attempt.tossOrderId}",
            )
        val subscription = charge.subscriptionId?.let { subscriptionRepository.findWithBillingMethodById(it) }
        if (!response.isSuccess2xx || response.json == null) {
            val message = response.json?.path("message")?.asText(null) ?: response.rawBody ?: "자동결제 승인 실패"
            attempt.markFailed(message, response.rawBody)
            charge.markFailed(message, response.rawBody)
            subscription?.markChargeFailed(now)
            return BillingChargeFinalizeResult(charge = charge, failureMessage = message)
        }
        val paymentKey = response.json.path("paymentKey").asText(null)
        val approvedAt = parseDateTime(response.json.path("approvedAt").asText(null)) ?: now
        attempt.markDone(paymentKey, response.rawBody, approvedAt)
        charge.markDone(paymentKey, response.rawBody, approvedAt)
        subscription?.markCharged(now)
        return BillingChargeFinalizeResult(charge = charge)
    }

    private fun savePendingAttempt(
        charge: TossBillingCharge,
        method: TossBillingMethod,
    ): TossBillingChargeAttempt {
        val attemptNo = (attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)?.attemptNo ?: 0) + 1
        return attemptRepository.save(
            TossBillingChargeAttempt(
                chargeId = charge.id,
                billingMethod = method,
                attemptNo = attemptNo,
                tossOrderId = BillingChargeExecutor.attemptOrderId(charge.orderId, attemptNo),
                status = TossBillingChargeStatus.PENDING,
            ),
        )
    }

    private fun reconcilePendingIfPaid(
        charge: TossBillingCharge,
        now: LocalDateTime,
    ): TossBillingCharge? {
        val latest = attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)
        val orderIds =
            buildList {
                if (latest != null) add(latest.tossOrderId)
                add(charge.orderId)
            }.distinct()
        return orderIds.firstNotNullOfOrNull { orderId ->
            val remote = tossPaymentsApiClient.getPaymentByOrderId(orderId)
            val done =
                remote.isSuccess2xx &&
                    remote.json != null &&
                    remote.json.path("status").asText(null) == "DONE"
            if (!done) {
                null
            } else {
                val paymentKey = remote.json!!.path("paymentKey").asText(null)
                val approvedAt = parseDateTime(remote.json.path("approvedAt").asText(null)) ?: now
                latest
                    ?.takeIf {
                        it.tossOrderId == orderId && it.status == TossBillingChargeStatus.PENDING
                    }?.markDone(paymentKey, remote.rawBody, approvedAt)
                charge.markDone(paymentKey, remote.rawBody, approvedAt)
                charge.subscriptionId
                    ?.let { subscriptionRepository.findWithBillingMethodById(it) }
                    ?.markCharged(now)
                log.info("PENDING 회차를 토스 조회로 확정했습니다. orderId={}", orderId)
                charge
            }
        }
    }

    private fun parseDateTime(raw: String?): LocalDateTime? = IdolGlowDateTimes.parseDateTime(raw)
}

data class PreparedBillingCharge(
    val charge: TossBillingCharge,
    val alreadyDone: Boolean,
)

data class BillingChargeFinalizeResult(
    val charge: TossBillingCharge,
    val failureMessage: String? = null,
)
