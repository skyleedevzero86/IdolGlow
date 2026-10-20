package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BillingChargeExecutor(
    private val billingChargeTxService: BillingChargeTxService,
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
        return complete(billingChargeTxService.finalizeWithToss(prepared.charge.id, now))
    }

    fun chargeOneOff(
        userId: Long,
        methodId: Long,
        amount: Long,
        orderName: String,
    ): TossBillingCharge {
        val charge = billingChargeTxService.createOneOffPending(userId, methodId, amount, orderName)
        return complete(billingChargeTxService.finalizeWithToss(charge.id, LocalDateTime.now()))
    }

    fun retryFailedCharge(
        chargeId: Long,
        method: TossBillingMethod,
        now: LocalDateTime = LocalDateTime.now(),
    ): TossBillingCharge {
        billingChargeTxService.prepareRetry(chargeId, method)
        return complete(billingChargeTxService.finalizeWithToss(chargeId, now, method))
    }

    private fun complete(result: BillingChargeFinalizeResult): TossBillingCharge {
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
}
