package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailUnsubscribeNotice
import org.springframework.stereotype.Component

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
        val linesHtml =
            lines.joinToString("") {
                "<p style=\"margin:10px 0 0;font-size:14px;line-height:1.8;color:#544b46;\">${escapeHtml(it)}</p>"
            }
        val linkHtml =
            myPageUrl
                ?.let {
                    "<p style=\"margin:20px 0 0;\"><a href=\"${escapeHtml(it)}\" style=\"color:#c8673d;\">" +
                        "마이페이지로 이동</a></p>"
                }.orEmpty()
        val htmlBody =
            buildString {
                append("<html><body style=\"margin:0;padding:0;background:#f6f1e8;color:#1f1a18;")
                append("font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;\">")
                append("<div style=\"max-width:560px;margin:0 auto;padding:32px 20px;\">")
                append("<div style=\"background:#fffaf4;border-radius:20px;border:1px solid #eadfce;padding:28px;\">")
                append("<h1 style=\"margin:0;font-size:22px;line-height:1.4;\">$TITLE</h1>")
                append("<p style=\"margin:16px 0 0;font-size:14px;color:#544b46;\">")
                append("처리 일시 ${escapeHtml(processedAt)}</p>")
                append(linesHtml)
                append(linkHtml)
                append("</div></div></body></html>")
            }
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
    }
}
