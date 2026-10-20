package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BillingChargeExecutor(
    private val billingChargeTxService: BillingChargeTxService,
) {
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
            throw IllegalStateException(message)
        }
        return result.charge
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
