package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BillingChargeExecutor(
    private val billingChargeTxService: BillingChargeTxService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val notificationCommandService: NotificationCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun chargeSubscription(
        subscriptionId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): TossBillingCharge {
        val prepared = billingChargeTxService.preparePeriodCharge(subscriptionId)
        if (prepared.alreadyDone) {
            return prepared.charge
        }
        return complete(finalize(prepared.charge.id, now, null))
    }

    fun chargeOneOff(
        userId: Long,
        methodId: Long,
        amount: Long,
        orderName: String,
    ): TossBillingCharge {
        val charge = billingChargeTxService.createOneOffPending(userId, methodId, amount, orderName)
        return complete(finalize(charge.id, LocalDateTime.now(), null))
    }

    fun retryFailedCharge(
        chargeId: Long,
        method: TossBillingMethod,
        now: LocalDateTime = LocalDateTime.now(),
    ): TossBillingCharge {
        billingChargeTxService.prepareRetry(chargeId, method)
        return complete(finalize(chargeId, now, method.id))
    }

    private fun finalize(
        chargeId: Long,
        now: LocalDateTime,
        methodId: Long?,
    ): BillingChargeFinalizeResult {
        val gate = billingChargeTxService.loadChargeGate(chargeId)
        val lookedUp =
            if (gate.status == TossBillingChargeStatus.DONE) {
                reload(chargeId, pending = false)
            } else {
                lookupPending(gate, now)
            }
        return when {
            lookedUp != null -> lookedUp
            gate.latestAttemptStatus == TossBillingChargeStatus.PENDING -> {
                log.warn(
                    "PENDING 시도가 있어 새 승인을 시작하지 않습니다. chargeId={} tossOrderId={}",
                    gate.chargeId,
                    gate.latestTossOrderId,
                )
                reload(chargeId, pending = true)
            }
            else -> approveOutside(chargeId, methodId, now)
        }
    }

    private fun lookupPending(
        gate: BillingChargeGate,
        now: LocalDateTime,
    ): BillingChargeFinalizeResult? {
        if (gate.status != TossBillingChargeStatus.PENDING) {
            return null
        }
        val orderIds =
            buildList {
                if (gate.latestTossOrderId != null) {
                    add(gate.latestTossOrderId)
                }
                add(gate.orderId)
            }.distinct()
        var found: BillingChargeFinalizeResult? = null
        for (orderId in orderIds) {
            if (found != null) {
                continue
            }
            found = lookupOrder(gate, orderId, now)
        }
        return found
    }

    private fun lookupOrder(
        gate: BillingChargeGate,
        orderId: String,
        now: LocalDateTime,
    ): BillingChargeFinalizeResult? {
        val remote = tossPaymentsApiClient.getPaymentByOrderId(orderId)
        return when (lookupKind(gate.amount, remote)) {
            LookupKind.AMOUNT_MISMATCH -> {
                log.error(
                    "정기결제 조회 금액이 회차 금액과 다릅니다. chargeId={} orderId={} amount={}",
                    gate.chargeId,
                    orderId,
                    gate.amount,
                )
                reload(gate.chargeId, pending = true)
            }
            LookupKind.MATCHED -> billingChargeTxService.applyLookup(gate.chargeId, orderId, remote, now)
            LookupKind.CONTINUE -> null
        }
    }

    private fun approveOutside(
        chargeId: Long,
        methodId: Long?,
        now: LocalDateTime,
    ): BillingChargeFinalizeResult {
        val prepared = billingChargeTxService.prepareAttempt(chargeId, methodId) ?: return reload(chargeId)
        val response =
            tossPaymentsApiClient.chargeBilling(
                billingKey = prepared.billingKey,
                customerKey = prepared.customerKey,
                amount = prepared.amount,
                orderId = prepared.tossOrderId,
                orderName = prepared.orderName,
                customerEmail = prepared.customerEmail,
                customerName = prepared.customerName,
                idempotencyKey = "billing-charge-${prepared.tossOrderId}",
            )
        return billingChargeTxService.applyChargeResult(prepared.attemptId, response, now)
    }

    private fun reload(
        chargeId: Long,
        pending: Boolean? = null,
    ): BillingChargeFinalizeResult {
        val charge = billingChargeTxService.requireCharge(chargeId)
        val stayPending = pending ?: (charge.status == TossBillingChargeStatus.PENDING)
        return BillingChargeFinalizeResult(charge = charge, pending = stayPending)
    }

    private fun lookupKind(
        amount: Long,
        remote: TossApiResponse,
    ): LookupKind {
        val json = remote.json
        val expected = BigDecimal.valueOf(amount)
        val matched =
            remote.isSuccess2xx &&
                json != null &&
                TossPaymentResponseMapper.isDoneWithMatchingAmount(json, expected)
        val doneWithOtherAmount =
            remote.isSuccess2xx &&
                json != null &&
                json.path("status").asText(null)?.trim() == "DONE" &&
                !matched
        return when {
            doneWithOtherAmount -> LookupKind.AMOUNT_MISMATCH
            matched -> LookupKind.MATCHED
            else -> LookupKind.CONTINUE
        }
    }

    private fun complete(result: BillingChargeFinalizeResult): TossBillingCharge {
        if (result.pending) {
            return result.charge
        }
        val message = result.failureMessage
        if (message != null) {
            notifyChargeFailed(result.charge.userId, message)
            throw IllegalStateException(message)
        }
        return result.charge
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
                message = "자동결제에 실패했습니다. 다른 카드로 다시 결제할 수 있습니다. ($message)",
                link = "/billing",
            )
        }.onFailure { error ->
            log.warn("빌링 실패 알림 발송 실패 userId={}", userId, error)
        }
    }

    companion object {
        private val PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")

        fun periodOrderId(
            subscriptionId: Long,
            dueAt: LocalDateTime,
        ): String = "sub-$subscriptionId-${dueAt.format(PERIOD_FORMAT)}"

        fun attemptOrderId(
            cycleOrderId: String,
            attemptNo: Int,
        ): String = "$cycleOrderId-a$attemptNo"
    }

    private enum class LookupKind {
        MATCHED,
        AMOUNT_MISMATCH,
        CONTINUE,
    }
}
