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
        require(subscription.billingMethod.usableForCharge()) {
            "삭제된 결제 수단으로는 자동결제할 수 없습니다. methodId=${subscription.billingMethod.id}"
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
        require(method.usableForCharge()) {
            "삭제된 결제 수단으로는 결제할 수 없습니다. methodId=$methodId"
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
    fun loadChargeGate(chargeId: Long): BillingChargeGate {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        val latest = attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)
        return BillingChargeGate(
            chargeId = charge.id,
            status = charge.status,
            orderId = charge.orderId,
            amount = charge.amount,
            latestAttemptStatus = latest?.status,
            latestTossOrderId = latest?.tossOrderId,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyLookup(
        chargeId: Long,
        orderId: String,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingChargeFinalizeResult {
        val charge = lockCharge(chargeId)
        val expectedAmount = BigDecimal.valueOf(charge.amount)
        val confirmed =
            remote.json?.takeIf {
                charge.status == TossBillingChargeStatus.PENDING &&
                    TossPaymentResponseMapper.isDoneWithMatchingAmount(it, expectedAmount)
            }
        if (confirmed != null) {
            val paymentKey = confirmed.path("paymentKey").asText(null)
            val approvedAt = parseDateTime(confirmed.path("approvedAt").asText(null)) ?: now
            val latest = attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)
            latest
                ?.takeIf { it.tossOrderId == orderId && it.status == TossBillingChargeStatus.PENDING }
                ?.markDone(paymentKey, remote.rawBody, approvedAt)
            charge.markDone(paymentKey, remote.rawBody, approvedAt)
            charge.subscriptionId
                ?.let { subscriptionRepository.findWithBillingMethodById(it) }
                ?.markCharged(now)
            log.info("PENDING 회차를 토스 조회로 확정했습니다. orderId={}", orderId)
        }
        val stayPending = charge.status == TossBillingChargeStatus.PENDING
        return BillingChargeFinalizeResult(charge = charge, pending = stayPending)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepareAttempt(
        chargeId: Long,
        methodId: Long?,
    ): PreparedBillingAttempt? {
        val charge = lockCharge(chargeId)
        if (methodId != null) {
            charge.billingMethod =
                methodRepository.findById(methodId).orElseThrow {
                    IllegalArgumentException("결제 수단을 찾을 수 없습니다. methodId=$methodId")
                }
        }
        val latest =
            if (charge.status == TossBillingChargeStatus.PENDING) {
                attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)
            } else {
                null
            }
        if (latest?.status == TossBillingChargeStatus.PENDING) {
            log.warn(
                "PENDING 시도가 있어 새 승인을 시작하지 않습니다. chargeId={} tossOrderId={}",
                charge.id,
                latest.tossOrderId,
            )
        }
        val pending = charge.status == TossBillingChargeStatus.PENDING
        val open = pending && latest?.status != TossBillingChargeStatus.PENDING
        return if (open) savePendingAttempt(charge, latest) else null
    }

    private fun savePendingAttempt(
        charge: TossBillingCharge,
        latest: TossBillingChargeAttempt?,
    ): PreparedBillingAttempt {
        val method = charge.billingMethod
        val attemptNo = (latest?.attemptNo ?: 0) + 1
        val attempt =
            attemptRepository.save(
                TossBillingChargeAttempt(
                    chargeId = charge.id,
                    billingMethod = method,
                    attemptNo = attemptNo,
                    tossOrderId = BillingChargeExecutor.attemptOrderId(charge.orderId, attemptNo),
                    status = TossBillingChargeStatus.PENDING,
                ),
            )
        val user =
            userRepository.findById(charge.userId)
                ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다. userId=${charge.userId}")
        return PreparedBillingAttempt(
            attemptId = attempt.id,
            tossOrderId = attempt.tossOrderId,
            billingKey = method.billingKey,
            customerKey = method.customerKey,
            amount = charge.amount,
            orderName = charge.orderName,
            customerEmail = user.email,
            customerName = user.nickname.value,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyChargeResult(
        attemptId: Long,
        remote: TossApiResponse,
        now: LocalDateTime,
    ): BillingChargeFinalizeResult {
        val chargeId =
            attemptRepository.findChargeIdById(attemptId)
                ?: throw IllegalArgumentException("결제 시도를 찾을 수 없습니다. attemptId=$attemptId")
        val charge = lockCharge(chargeId)
        val attempt =
            attemptRepository.findById(attemptId).orElseThrow {
                IllegalArgumentException("결제 시도를 찾을 수 없습니다. attemptId=$attemptId")
            }
        val alreadySettled =
            charge.status != TossBillingChargeStatus.PENDING ||
                attempt.status != TossBillingChargeStatus.PENDING
        if (alreadySettled) {
            val stillPending = charge.status == TossBillingChargeStatus.PENDING
            return BillingChargeFinalizeResult(charge = charge, pending = stillPending)
        }
        val subscription = charge.subscriptionId?.let { subscriptionRepository.findWithBillingMethodById(it) }
        val json = remote.json
        val expectedAmount = BigDecimal.valueOf(charge.amount)
        return when {
            remote.isInconclusive -> {
                log.warn(
                    "정기결제 승인 결과가 불명확합니다. chargeId={} httpStatus={}",
                    charge.id,
                    remote.httpStatus,
                )
                BillingChargeFinalizeResult(charge = charge, pending = true)
            }
            remote.isSuccess2xx &&
                json != null &&
                TossPaymentResponseMapper.isDoneWithMatchingAmount(json, expectedAmount) -> {
                val paymentKey = json.path("paymentKey").asText(null)
                val approvedAt = parseDateTime(json.path("approvedAt").asText(null)) ?: now
                attempt.markDone(paymentKey, remote.rawBody, approvedAt)
                charge.markDone(paymentKey, remote.rawBody, approvedAt)
                subscription?.markCharged(now)
                BillingChargeFinalizeResult(charge = charge)
            }
            remote.isSuccess2xx -> {
                log.error(
                    "정기결제 승인 응답이 완료·금액 일치가 아닙니다. chargeId={} httpStatus={}",
                    charge.id,
                    remote.httpStatus,
                )
                BillingChargeFinalizeResult(charge = charge, pending = true)
            }
            else -> {
                val message = json?.path("message")?.asText(null) ?: remote.rawBody ?: "자동결제 승인 실패"
                attempt.markFailed(message, remote.rawBody)
                charge.markFailed(message, remote.rawBody)
                subscription?.markChargeFailed(now)
                BillingChargeFinalizeResult(charge = charge, failureMessage = message)
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun requireCharge(chargeId: Long): TossBillingCharge =
        chargeRepository.findById(chargeId).orElseThrow {
            IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
        }

    private fun lockCharge(chargeId: Long): TossBillingCharge =
        chargeRepository.findByIdForUpdate(chargeId)
            ?: throw IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")

    private fun parseDateTime(raw: String?): LocalDateTime? = IdolGlowDateTimes.parseDateTime(raw)
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

data class BillingChargeGate(
    val chargeId: Long,
    val status: TossBillingChargeStatus,
    val orderId: String,
    val amount: Long,
    val latestAttemptStatus: TossBillingChargeStatus?,
    val latestTossOrderId: String?,
)

data class PreparedBillingAttempt(
    val attemptId: Long,
    val tossOrderId: String,
    val billingKey: String,
    val customerKey: String,
    val amount: Long,
    val orderName: String,
    val customerEmail: String,
    val customerName: String,
)
