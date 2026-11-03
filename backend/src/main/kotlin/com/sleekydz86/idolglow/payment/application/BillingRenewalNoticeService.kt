package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.payment.application.dto.RenewalNoticeResponse
import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class BillingRenewalNoticeService(
    private val txService: BillingRenewalNoticeTxService,
    private val appMailProperties: AppMailProperties,
    private val outboundMailPort: OutboundMailPort,
    private val userRepository: UserRepository,
    @param:Value("\${app.frontend-base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
    @param:Value("\${payment.toss.billing.renewal-notice-days-before:7}")
    private val daysBefore: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendDueNotices(now: LocalDateTime = LocalDateTime.now()): Int {
        if (!appMailProperties.enabled) {
            return 0
        }
        var sent = 0
        txService.findTargetIds(now, daysBefore).forEach { subscriptionId ->
            runCatching {
                val prepared = txService.prepare(subscriptionId) ?: return@runCatching
                if (send(prepared)) {
                    txService.markSent(prepared.noticeId, now)
                    sent += 1
                }
            }.onFailure { error ->
                log.warn("갱신 안내 메일 준비 실패: subscriptionId={}", subscriptionId, error)
            }
        }
        return sent
    }

    fun preview(
        token: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): RenewalNoticeResponse = txService.preview(token, now)

    fun decide(
        token: String,
        decision: RenewalDecision,
        now: LocalDateTime = LocalDateTime.now(),
    ): RenewalNoticeResponse = txService.decide(token, decision, now)

    private fun send(prepared: PreparedRenewalNotice): Boolean {
        val user = userRepository.findById(prepared.userId) ?: return false
        val cycle = BillingCycle.ofMonths(prepared.cycleMonths)
        val date = prepared.periodEnd.format(DATE_FORMAT)
        val amount = "${NumberFormat.getNumberInstance(Locale.KOREA).format(prepared.amount)}원"
        val link = "${frontendBaseUrl.trim().trimEnd('/')}/billing/renewal?token=${prepared.token}"
        val subject = "[IdolGlow] Glow Pass ${cycle.label} 이용 기간이 $date 에 끝나요"
        val plan =
            if (prepared.autoRenew) {
                "$date 에 같은 카드로 $amount 이 결제되고 ${cycle.label} 이 연장됩니다."
            } else {
                "이번 기간만 이용하기로 하셔서 $date 에 결제 없이 종료됩니다."
            }
        val plainText =
            buildString {
                appendLine("Glow Pass ${cycle.label} 이용 기간 안내")
                appendLine()
                appendLine(plan)
                appendLine("계속 이용할지, 이번 기간만 쓰고 끝낼지 아래 링크에서 고를 수 있어요.")
                appendLine(link)
                appendLine()
                appendLine("따로 고르지 않으면 지금 설정대로 진행됩니다.")
            }
        val htmlBody =
            """
            <html>
            <body style="font-family:'Malgun Gothic',Arial,sans-serif;color:#1a1a2e;">
              <h2 style="margin:0 0 16px;">Glow Pass ${escapeHtml(cycle.label)} 이용 기간 안내</h2>
              <p style="margin:0 0 12px;">${escapeHtml(plan)}</p>
              <p style="margin:0 0 20px;">계속 이용할지, 이번 기간만 쓰고 끝낼지 아래에서 고를 수 있어요.</p>
              <p style="margin:0 0 20px;">
                <a href="${escapeHtml(link)}" style="display:inline-block;padding:12px 20px;background:#e94560;color:#fff;text-decoration:none;font-weight:700;">연장 여부 선택하기</a>
              </p>
              <p style="margin:0;color:#8888a0;font-size:13px;">따로 고르지 않으면 지금 설정대로 진행됩니다.</p>
            </body>
            </html>
            """.trimIndent()
        return runCatching {
            outboundMailPort.send(
                OutboundMailMessage(
                    to = user.email,
                    subject = subject,
                    plainTextBody = plainText,
                    htmlBody = htmlBody,
                ),
            )
            true
        }.getOrElse { error ->
            log.error("갱신 안내 메일 발송 실패: noticeId={}", prepared.noticeId, error)
            false
        }
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}
