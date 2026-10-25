package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.PaymentLog
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PaymentLogStuckAlertClaim(
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
    private val paymentLogTxService: PaymentLogTxService,
) : StuckPaymentAlertClaim {
    override fun alreadySent(
        subjectKey: String,
        day: LocalDate,
    ): Boolean = paymentLogJpaRepository.existsByErrorCode(marker(subjectKey, day))

    override fun markSent(
        subjectKey: String,
        day: LocalDate,
        alert: StuckPaymentAlert,
    ) {
        paymentLogTxService.saveDetached(alert.linkedPaymentId, logFor(subjectKey, day, alert))
    }

    private fun logFor(
        subjectKey: String,
        day: LocalDate,
        alert: StuckPaymentAlert,
    ): PaymentLog =
        PaymentLog(
            orderId = alert.orderId,
            logType = PaymentLogType.SYSTEM,
            step = PaymentLogStep.SERVER,
            responseBody = alert.lastTossLookup.take(LOG_LIMIT),
            errorCode = marker(subjectKey, day),
            errorMessage = alert.describe().take(LOG_LIMIT),
        )

    private fun marker(
        subjectKey: String,
        day: LocalDate,
    ): String {
        val value = "stuck:$subjectKey:$day"
        require(value.length <= MARKER_LIMIT) { "알림 키가 너무 깁니다. subjectKey=$subjectKey" }
        return value
    }

    companion object {
        private const val LOG_LIMIT = 1000
        private const val MARKER_LIMIT = 100
    }
}
