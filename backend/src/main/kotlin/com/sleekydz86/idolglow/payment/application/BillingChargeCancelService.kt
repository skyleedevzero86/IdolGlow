package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.TossBillingChargeResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BillingChargeCancelService(
    private val txService: BillingChargeCancelTxService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val chargeRepository: TossBillingChargeJpaRepository,
) {
    fun cancel(
        userId: Long,
        chargeId: Long,
        reason: String?,
        now: LocalDateTime = LocalDateTime.now(),
    ): TossBillingChargeResponse {
        val prepared = txService.prepare(userId, chargeId, now)
        val response =
            tossPaymentsApiClient.cancel(
                paymentKey = prepared.paymentKey,
                cancelReason = reason?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_REASON,
                cancelAmount = null,
                idempotencyKey = "billing-charge-cancel-${prepared.chargeId}",
            )
        check(response.isSuccess2xx) {
            "결제 취소에 실패했습니다. 잠시 뒤 다시 시도해 주세요. status=${response.httpStatus}"
        }
        txService.complete(prepared.chargeId, now)
        return TossBillingChargeResponse.from(chargeRepository.findById(prepared.chargeId).orElseThrow())
    }

    companion object {
        private const val DEFAULT_REASON = "고객 요청 취소"
    }
}
