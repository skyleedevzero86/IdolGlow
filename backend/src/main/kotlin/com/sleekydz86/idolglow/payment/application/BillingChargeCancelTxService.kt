package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class PreparedChargeCancel(
    val chargeId: Long,
    val paymentKey: String,
)

@Service
class BillingChargeCancelTxService(
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
) {
    @Transactional(readOnly = true)
    fun prepare(
        userId: Long,
        chargeId: Long,
        now: LocalDateTime,
    ): PreparedChargeCancel {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        require(charge.userId == userId) { "본인 결제 회차만 취소할 수 있습니다. chargeId=$chargeId" }
        charge.assertCancelable(now)
        return PreparedChargeCancel(chargeId = charge.id, paymentKey = charge.paymentKey!!)
    }

    @Transactional
    fun complete(
        chargeId: Long,
        now: LocalDateTime,
    ) {
        val charge =
            chargeRepository.findById(chargeId).orElseThrow {
                IllegalArgumentException("결제 회차를 찾을 수 없습니다. chargeId=$chargeId")
            }
        if (charge.status == TossBillingChargeStatus.CANCELED) {
            return
        }
        charge.markCanceled(now)
        val subscriptionId = charge.subscriptionId ?: return
        subscriptionRepository.findById(subscriptionId).ifPresent { it.cancelImmediately() }
    }
}
