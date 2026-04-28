package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.config.AppMailProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentNotificationMailService(
    private val appMailProperties: AppMailProperties,
    private val outboundMailPort: OutboundMailPort,
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(PaymentNotificationMailService::class.java)

    fun sendSucceeded(payment: Payment) {
        send(payment, "결제가 정상적으로 완료되었습니다.", "결제 성공")
    }

    fun sendFailed(payment: Payment) {
        send(payment, payment.failureReason ?: "결제 처리에 실패했습니다.", "결제 실패")
    }

    fun sendCanceled(payment: Payment) {
        send(payment, "결제 취소가 완료되었습니다.", "결제 취소")
    }

    private fun send(payment: Payment, detail: String, title: String) {
        if (!appMailProperties.enabled) {
            return
        }
        val user = userRepository.findById(payment.reservation.userId) ?: return
        val productName = payment.reservation.reservationSlot.product.name
        val visitLine =
            "${payment.reservation.visitDate} ${payment.reservation.visitStartTime} ~ ${payment.reservation.visitEndTime}"
        val subject = "[IdolGlow] $title 안내"
        val plainText = buildString {
            appendLine("$title 안내")
            appendLine()
            appendLine("상품명: $productName")
            appendLine("방문일시: $visitLine")
            appendLine("결제참조: ${payment.paymentReference}")
            appendLine("상태: ${payment.status.name}")
            appendLine("결제금액: ${payment.amount.toPlainString()} KRW")
            appendLine("취소금액: ${payment.cancelAmount.toPlainString()} KRW")
            appendLine("안내: $detail")
        }
        val htmlBody = """
            <html>
            <body style="font-family:'Malgun Gothic',Arial,sans-serif;color:#111827;">
              <h2 style="margin:0 0 16px;">$title 안내</h2>
              <p style="margin:0 0 8px;">상품명: <strong>${escapeHtml(productName)}</strong></p>
              <p style="margin:0 0 8px;">방문일시: <strong>${escapeHtml(visitLine)}</strong></p>
              <p style="margin:0 0 8px;">결제참조: <strong>${escapeHtml(payment.paymentReference)}</strong></p>
              <p style="margin:0 0 8px;">상태: <strong>${payment.status.name}</strong></p>
              <p style="margin:0 0 8px;">결제금액: <strong>${payment.amount.toPlainString()} KRW</strong></p>
              <p style="margin:0 0 8px;">취소금액: <strong>${payment.cancelAmount.toPlainString()} KRW</strong></p>
              <p style="margin:20px 0 0;">${escapeHtml(detail)}</p>
            </body>
            </html>
        """.trimIndent()

        runCatching {
            outboundMailPort.send(
                OutboundMailMessage(
                    to = user.email,
                    subject = subject,
                    plainTextBody = plainText,
                    htmlBody = htmlBody,
                )
            )
        }.onFailure { ex ->
            log.error("결제 결과 메일 발송 실패: paymentId={}", payment.id, ex)
        }
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
