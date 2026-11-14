package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate

object PaymentAdminNotifications {
    const val ADMIN_PAYMENTS_LINK = "/admin/payments"
    private const val ORDER_ID_LIMIT = 80
    private const val SUMMARY_LIMIT = 300
    private val SAFE_KEY = Regex("[^A-Za-z0-9_-]")

    fun completed(
        amount: BigDecimal,
        orderId: String,
    ): AdminNotificationRequestedEvent =
        AdminNotificationRequestedEvent(
            type = NotificationType.ADMIN_PAYMENT_COMPLETED,
            title = "결제 완료",
            message = "${formatAmount(amount)}원 결제가 완료되었어요. (주문 ${orderId.take(ORDER_ID_LIMIT)})",
            link = ADMIN_PAYMENTS_LINK,
        )

    fun completed(charge: TossBillingCharge): AdminNotificationRequestedEvent =
        completed(BigDecimal.valueOf(charge.amount), charge.orderId)

    fun issue(
        alert: StuckPaymentAlert,
        day: LocalDate,
    ): AdminNotificationRequestedEvent =
        AdminNotificationRequestedEvent(
            type = NotificationType.ADMIN_PAYMENT_ISSUE,
            title = "결제 확인 필요",
            message = "결제 확인이 필요한 건이 있어요: ${alert.describe().take(SUMMARY_LIMIT)}",
            link = "$ADMIN_PAYMENTS_LINK#issue-${alert.subjectKey.replace(SAFE_KEY, "-")}-$day",
            deduplicateByLink = true,
        )

    private fun formatAmount(amount: BigDecimal): String =
        DecimalFormat("#,###").format(amount.setScale(0, RoundingMode.DOWN))
}
