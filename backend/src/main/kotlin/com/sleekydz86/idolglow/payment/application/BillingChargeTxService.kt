package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
        val reconciled =
            if (charge.status == TossBillingChargeStatus.PENDING) {
                reconcilePendingIfPaid(charge, now)
            } else {
                null
            }
        val openAttempt = openPendingAttempt(charge)
        return when {
            charge.status == TossBillingChargeStatus.DONE -> BillingChargeFinalizeResult(charge = charge)
            reconciled != null -> BillingChargeFinalizeResult(charge = reconciled)
            openAttempt != null -> {
                log.warn(
                    "PENDING 시도가 있어 새 승인을 시작하지 않습니다. chargeId={} tossOrderId={}",
                    charge.id,
                    openAttempt.tossOrderId,
                )
                BillingChargeFinalizeResult(charge = charge, pending = true)
            }
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
        val json = response.json
        val expectedAmount = BigDecimal.valueOf(charge.amount)
        return when {
            response.isInconclusive -> {
                log.warn(
                    "정기결제 승인 결과가 불명확합니다. chargeId={} httpStatus={}",
                    charge.id,
                    response.httpStatus,
                )
                BillingChargeFinalizeResult(charge = charge, pending = true)
            }
            response.isSuccess2xx &&
                json != null &&
                TossPaymentResponseMapper.isDoneWithMatchingAmount(json, expectedAmount) -> {
                val paymentKey = json.path("paymentKey").asText(null)
                val approvedAt = parseDateTime(json.path("approvedAt").asText(null)) ?: now
                attempt.markDone(paymentKey, response.rawBody, approvedAt)
                charge.markDone(paymentKey, response.rawBody, approvedAt)
                subscription?.markCharged(now)
                BillingChargeFinalizeResult(charge = charge)
            }
            response.isSuccess2xx -> {
                log.error(
                    "정기결제 승인 응답이 완료·금액 일치가 아닙니다. chargeId={} httpStatus={}",
                    charge.id,
                    response.httpStatus,
                )
                BillingChargeFinalizeResult(charge = charge, pending = true)
            }
            else -> {
                val message = json?.path("message")?.asText(null) ?: response.rawBody ?: "자동결제 승인 실패"
                attempt.markFailed(message, response.rawBody)
                charge.markFailed(message, response.rawBody)
                subscription?.markChargeFailed(now)
                BillingChargeFinalizeResult(charge = charge, failureMessage = message)
            }
        }
    }

    private fun openPendingAttempt(charge: TossBillingCharge): TossBillingChargeAttempt? {
        val latest =
            if (charge.status == TossBillingChargeStatus.PENDING) {
                attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)
            } else {
                null
            }
        return latest?.takeIf { it.status == TossBillingChargeStatus.PENDING }
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
        val hit =
            orderIds.firstNotNullOfOrNull { orderId ->
                reconcileOrder(charge, latest, orderId, now)
            }
        return hit?.takeUnless { it.stop }?.charge
    }

    private fun reconcileOrder(
        charge: TossBillingCharge,
        latest: TossBillingChargeAttempt?,
        orderId: String,
        now: LocalDateTime,
    ): ReconcileHit? {
        val remote = tossPaymentsApiClient.getPaymentByOrderId(orderId)
        val json = remote.json
        val expectedAmount = BigDecimal.valueOf(charge.amount)
        val matched =
            remote.isSuccess2xx &&
                json != null &&
                TossPaymentResponseMapper.isDoneWithMatchingAmount(json, expectedAmount)
        val doneWithOtherAmount =
            remote.isSuccess2xx &&
                json != null &&
                json.path("status").asText(null)?.trim() == "DONE" &&
                !matched
        return when {
            doneWithOtherAmount -> {
                log.error(
                    "정기결제 조회 금액이 회차 금액과 다릅니다. chargeId={} orderId={} amount={}",
                    charge.id,
                    orderId,
                    charge.amount,
                )
                ReconcileHit(charge = null, stop = true)
            }
            matched -> {
                markReconciled(charge, latest, orderId, remote, now)
                ReconcileHit(charge = charge, stop = false)
            }
            else -> null
        }
    }

    private fun markReconciled(
        charge: TossBillingCharge,
        latest: TossBillingChargeAttempt?,
        orderId: String,
        remote: TossApiResponse,
        now: LocalDateTime,
    ) {
        val json = remote.json ?: return
        val paymentKey = json.path("paymentKey").asText(null)
        val approvedAt = parseDateTime(json.path("approvedAt").asText(null)) ?: now
        latest
            ?.takeIf { it.tossOrderId == orderId && it.status == TossBillingChargeStatus.PENDING }
            ?.markDone(paymentKey, remote.rawBody, approvedAt)
        charge.markDone(paymentKey, remote.rawBody, approvedAt)
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.markCharged(now)
        log.info("PENDING 회차를 토스 조회로 확정했습니다. orderId={}", orderId)
    }

    private fun parseDateTime(raw: String?): LocalDateTime? = IdolGlowDateTimes.parseDateTime(raw)

    private data class ReconcileHit(
        val charge: TossBillingCharge?,
        val stop: Boolean,
    )
}

data class PreparedBillingCharge(
    val charge: TossBillingCharge,
    val alreadyDone: Boolean,
)

data class BillingChargeFinalizeResult(
    val charge: TossBillingCharge,
    val failureMessage: String? = null,
    val pending: Boolean = false,
)
