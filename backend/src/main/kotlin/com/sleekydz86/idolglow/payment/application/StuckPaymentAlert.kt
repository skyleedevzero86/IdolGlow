package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class StuckPaymentAlert(
    val subjectKey: String,
    val paymentId: String,
    val orderId: String,
    val status: String,
    val stuckSince: LocalDateTime,
    val lastTossLookup: String,
    val linkedPaymentId: Long? = null,
) {
    fun describe(now: LocalDateTime = LocalDateTime.now()): String {
        val minutes = Duration.between(stuckSince, now).toMinutes().coerceAtLeast(0)
        return "결제 ID: $paymentId, 주문 ID: $orderId, 상태: $status, " +
            "멈춘 시간: ${minutes}분, 마지막 토스 조회 결과: $lastTossLookup"
    }
}

interface StuckPaymentAlertClaim {
    fun alreadySent(
        subjectKey: String,
        day: LocalDate,
    ): Boolean

    fun markSent(
        subjectKey: String,
        day: LocalDate,
        alert: StuckPaymentAlert,
    )
}

fun interface StuckPaymentAdminNotifier {
    fun notify(alert: StuckPaymentAlert): Boolean
}

fun tossLookupSummary(remote: TossApiResponse): String {
    val status = tossField(remote, "status")
    val code = tossField(remote, "code")
    val message = tossField(remote, "message").take(TOSS_MESSAGE_LIMIT)
    return listOfNotNull(
        "http=${remote.httpStatus}",
        status.takeIf { it.isNotEmpty() }?.let { "status=$it" },
        code.takeIf { it.isNotEmpty() }?.let { "code=$it" },
        message.takeIf { it.isNotEmpty() }?.let { "message=$it" },
    ).joinToString(" ")
}

private fun tossField(
    remote: TossApiResponse,
    field: String,
): String {
    val node = remote.json?.path(field)
    val text = if (node == null || node.isMissingNode || node.isNull) "" else node.asText()
    return text.trim()
}

private const val TOSS_MESSAGE_LIMIT = 120
