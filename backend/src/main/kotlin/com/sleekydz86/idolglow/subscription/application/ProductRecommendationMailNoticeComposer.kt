package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeNotice
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import com.sleekydz86.idolglow.subscription.application.ProductRecommendationMailLayout as Layout

@Component
class ProductRecommendationMailNoticeComposer(
    private val appMailProperties: AppMailProperties,
    private val appPublicUrlProperties: AppPublicUrlProperties,
) {
    fun composeUnsubscribed(notice: ProductRecommendationMailUnsubscribeNotice): ComposedSubscriptionEmail {
        val senderName = appMailProperties.fromName.trim().ifEmpty { DEFAULT_SENDER_NAME }
        val subject = "[$senderName] 상품 추천 메일 수신 거부가 완료되었습니다"
        val processedAt = requireNotNull(IdolGlowDateTimes.toDisplay(notice.unsubscribedAt))
        val myPageUrl = resolveUrl(MY_PAGE_PATH)
        val lines =
            listOf(
                "앞으로 $senderName 상품 추천 메일은 발송되지 않습니다.",
                "소식지·웹진 구독과 서비스 이용에 필요한 안내 메일은 이 설정과 관계없이 발송될 수 있습니다.",
                "다시 받으려면 마이페이지에서 언제든지 수신 동의를 할 수 있습니다.",
            )
        val plainText =
            buildString {
                appendLine(TITLE)
                appendLine()
                appendLine("처리 일시: $processedAt")
                lines.forEach(::appendLine)
                if (myPageUrl != null) {
                    appendLine()
                    appendLine("마이페이지: $myPageUrl")
                }
            }.trim()
        val htmlBody =
            Layout.document(
                body =
                    buildString {
                        append(Layout.heading(TITLE))
                        append(Layout.highlight(listOf("처리 일시 ${escapeHtml(processedAt)}")))
                        lines.forEach { append(Layout.paragraph(escapeHtml(it))) }
                        myPageUrl?.let { append(Layout.button(escapeHtml(it), "마이페이지로 이동")) }
                    },
            )
        return ComposedSubscriptionEmail(
            subject = subject,
            plainText = plainText,
            htmlBody = htmlBody,
        )
    }

    fun composeConsentReminder(
        recipient: String,
        consentedAt: LocalDateTime,
        unsubscribeUrl: String,
    ): ComposedSubscriptionEmail {
        val settings = appMailProperties.recommendation
        val senderName = settings.senderName.trim().ifEmpty { appMailProperties.fromName.trim() }
        val senderContact =
            settings.senderContact
                .trim()
                .ifEmpty { appMailProperties.replyTo.trim() }
                .ifEmpty { appMailProperties.fromAddress.trim() }
        val subject =
            listOf(settings.subjectPrefix.trim(), CONSENT_REMINDER_SUBJECT)
                .filter { it.isNotEmpty() }
                .joinToString(" ")
        val consentDate = requireNotNull(IdolGlowDateTimes.toDisplay(consentedAt.toLocalDate()))
        val lines =
            listOf(
                "$recipient 주소는 $consentDate 에 상품 추천 메일(광고성 정보) 수신에 동의하셨습니다.",
                "정보통신망법에 따라 수신 동의일로부터 2년마다 수신 동의 상태를 알려 드립니다.",
                "계속 받으시려면 따로 하실 일이 없습니다. 지금처럼 상품 추천 메일을 보내 드립니다.",
                "더 이상 받지 않으려면 아래 수신 거부 링크를 눌러 주세요. 로그인 없이 바로 처리됩니다.",
            )
        val plainText =
            buildString {
                appendLine(CONSENT_REMINDER_TITLE)
                appendLine()
                appendLine("수신 동의일: $consentDate")
                lines.forEach(::appendLine)
                appendLine()
                appendLine("수신 거부: $unsubscribeUrl")
                appendLine("보내는 곳: $senderName / 연락처: $senderContact")
            }.trim()
        val htmlBody =
            Layout.document(
                body =
                    buildString {
                        append(Layout.heading(CONSENT_REMINDER_TITLE))
                        append(Layout.highlight(listOf("수신 동의일 ${escapeHtml(consentDate)}")))
                        lines.forEach { append(Layout.paragraph(escapeHtml(it))) }
                        append(Layout.button(escapeHtml(unsubscribeUrl), "수신 거부"))
                    },
                footer = Layout.footnote("보내는 곳: ${escapeHtml(senderName)} · 연락처: ${escapeHtml(senderContact)}"),
            )
        return ComposedSubscriptionEmail(
            subject = subject,
            plainText = plainText,
            htmlBody = htmlBody,
        )
    }

    private fun resolveUrl(path: String): String? {
        val base = appPublicUrlProperties.publicBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return null
        return "$base$path"
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    companion object {
        private const val TITLE = "상품 추천 메일 수신 거부가 완료되었습니다."
        private const val DEFAULT_SENDER_NAME = "IdolGlow"
        private const val MY_PAGE_PATH = "/mypage"
        private const val CONSENT_REMINDER_TITLE = "상품 추천 메일 수신 동의 상태를 알려 드립니다."
        private const val CONSENT_REMINDER_SUBJECT = "상품 추천 메일 수신 동의 상태 안내"
    }
}
