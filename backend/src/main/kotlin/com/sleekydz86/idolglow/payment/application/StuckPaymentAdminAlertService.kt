package com.sleekydz86.idolglow.payment.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StuckPaymentAdminAlertService(
    private val claim: StuckPaymentAlertClaim,
    private val notifier: StuckPaymentAdminNotifier,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyOncePerDay(alert: StuckPaymentAlert) {
        val day = LocalDate.now()
        if (claim.alreadySent(alert.subjectKey, day)) {
            return
        }
        if (!send(alert)) {
            return
        }
        remember(alert, day)
    }

    private fun send(alert: StuckPaymentAlert): Boolean =
        runCatching { notifier.notify(alert) }
            .onFailure { error ->
                log.warn("관리자 멈춤 알림 발송에 실패했습니다. subjectKey={}", alert.subjectKey, error)
            }.getOrDefault(false)

    private fun remember(
        alert: StuckPaymentAlert,
        day: LocalDate,
    ) {
        runCatching { claim.markSent(alert.subjectKey, day, alert) }
            .onFailure { error ->
                log.warn("관리자 멈춤 알림 기록에 실패했습니다. subjectKey={}", alert.subjectKey, error)
            }
    }
}
