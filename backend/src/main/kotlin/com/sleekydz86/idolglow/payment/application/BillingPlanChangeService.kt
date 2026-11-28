package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.BillingPlanChangeExceptionType
import com.sleekydz86.idolglow.payment.application.dto.PlanChangePreviewResponse
import com.sleekydz86.idolglow.payment.application.dto.PlanChangeResultResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BillingPlanChangeService(
    private val txService: BillingPlanChangeTxService,
    private val billingChargeExecutor: BillingChargeExecutor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun preview(
        userId: Long,
        cycleMonths: Int,
        now: LocalDateTime = LocalDateTime.now(),
    ): PlanChangePreviewResponse = txService.preview(userId, cycleMonths, now)

    fun change(
        userId: Long,
        command: PlanChangeCommand,
        now: LocalDateTime = LocalDateTime.now(),
    ): PlanChangeResultResponse {
        val prepared = txService.prepare(userId, command, now)
        val chargeId = prepared.chargeId ?: return txService.result(prepared.changeId)
        val settled = billingChargeExecutor.finalizeCharge(chargeId, now, null)
        if (settled.failureMessage != null) {
            log.warn(
                "이용권 변경 차액 결제 실패 userId={} changeId={} chargeId={} message={}",
                userId,
                prepared.changeId,
                chargeId,
                settled.failureMessage,
            )
            throw CustomException(BillingPlanChangeExceptionType.PAYMENT_FAILED)
        }
        return txService.result(prepared.changeId)
    }
}
