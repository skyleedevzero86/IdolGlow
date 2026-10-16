package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class BillingChargeExecutor(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val methodRepository: TossBillingMethodJpaRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val userRepository: UserRepository,
    private val notificationCommandService: NotificationCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun chargeSubscription(
        subscriptionId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): TossBillingCharge {
        val prepared = preparePeriodCharge(subscriptionId, now)
        if (prepared.alreadyDone) {
            return prepared.charge
        }
        return finalizeWithToss(prepared.charge.id, now)
    }

    fun chargeOneOff(
        userId: Long,
        methodId: Long,
        amount: Long,
        orderName: String,
    ): TossBillingCharge {
        val charge = createOneOffPending(userId, methodId, amount, orderName)
        return finalizeWithToss(charge.id, LocalDateTime.now())
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun preparePeriodCharge(
        subscriptionId: Long,
        now: LocalDateTime,
    ): PreparedCharge {
        val subscription =
            subscriptionRepository.findWithBillingMethodById(subscriptionId)
                ?: throw IllegalArgumentException("구독을 찾을 수 없습니다. subscriptionId=$subscriptionId")
        require(subscription.status == TossBillingSubscriptionStatus.ACTIVE) {
            "ACTIVE 구독만 자동결제할 수 있습니다. status=${subscription.status}"
        }
        val dueAt =
            subscription.nextBillingAt
                ?: throw IllegalStateException("nextBillingAt 이 비어 있습니다. subscriptionId=$subscriptionId")
        val orderId = periodOrderId(subscription.id, dueAt)
        val existing = chargeRepository.findByOrderId(orderId)
        if (existing != null && existing.status == TossBillingChargeStatus.DONE) {
            return PreparedCharge(charge = existing, alreadyDone = true)
        }
        if (existing != null) {
            if (existing.status == TossBillingChargeStatus.FAILED) {
                existing.clearFailureForRetry()
            }
            return PreparedCharge(charge = existing, alreadyDone = false)
        }
        val created =
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
        return PreparedCharge(charge = created, alreadyDone = false)
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
        val orderId = "billing-$userId-${UUID.randomUUID().toString().replace("-", "").take(20)}"
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
    fun finalizeWithToss(
        chargeId: Long,
        now: LocalDateTime,
    ): TossBillingCharge {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        if (charge.status == TossBillingChargeStatus.DONE) {
            return charge
        }

        if (charge.status == TossBillingChargeStatus.PENDING) {
            val reconciled = reconcilePendingIfPaid(charge, now)
            if (reconciled != null) {
                return reconciled
            }
        }

        val user =
            userRepository.findById(charge.userId)
                ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다. userId=${charge.userId}")
        val method = charge.billingMethod
        val response =
            tossPaymentsApiClient.chargeBilling(
                billingKey = method.billingKey,
                customerKey = method.customerKey,
                amount = charge.amount,
                orderId = charge.orderId,
                orderName = charge.orderName,
                customerEmail = user.email,
                customerName = user.nickname.value,
                idempotencyKey = "billing-charge-${charge.orderId}",
            )

        val subscription =
            charge.subscriptionId?.let { subscriptionRepository.findWithBillingMethodById(it) }

        if (!response.isSuccess2xx || response.json == null) {
            val message = response.json?.path("message")?.asText(null) ?: response.rawBody ?: "자동결제 승인 실패"
            charge.markFailed(message, response.rawBody)
            subscription?.markChargeFailed(now)
            notifyChargeFailed(charge.userId, message)
            throw IllegalStateException(message)
        }

        val paymentKey = response.json.path("paymentKey").asText(null)
        charge.markDone(
            paymentKey,
            response.rawBody,
            parseDateTime(response.json.path("approvedAt").asText(null)) ?: now,
        )
        subscription?.markCharged(now)
        return charge
    }

    private fun reconcilePendingIfPaid(
        charge: TossBillingCharge,
        now: LocalDateTime,
    ): TossBillingCharge? {
        val remote = tossPaymentsApiClient.getPaymentByOrderId(charge.orderId)
        if (!remote.isSuccess2xx || remote.json == null) {
            return null
        }
        val status = remote.json.path("status").asText(null)
        if (status != "DONE") {
            return null
        }
        val paymentKey = remote.json.path("paymentKey").asText(null)
        charge.markDone(
            paymentKey,
            remote.rawBody,
            parseDateTime(remote.json.path("approvedAt").asText(null)) ?: now,
        )
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.markCharged(now)
        log.info("PENDING 회차를 토스 조회로 확정했습니다. orderId={}", charge.orderId)
        return charge
    }

    private fun notifyChargeFailed(
        userId: Long,
        message: String,
    ) {
        runCatching {
            notificationCommandService.create(
                userId = userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "Glow Pass 결제 실패",
                message = "자동결제에 실패했습니다. 카드를 확인해 주세요. ($message)",
                link = "/billing",
            )
        }.onFailure { error ->
            log.warn("빌링 실패 알림 발송 실패 userId={}", userId, error)
        }
    }

    private fun parseDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }

    data class PreparedCharge(
        val charge: TossBillingCharge,
        val alreadyDone: Boolean,
    )

    companion object {
        private val PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")

        fun periodOrderId(
            subscriptionId: Long,
            dueAt: LocalDateTime,
        ): String = "sub-$subscriptionId-${dueAt.format(PERIOD_FORMAT)}"
    }
}
