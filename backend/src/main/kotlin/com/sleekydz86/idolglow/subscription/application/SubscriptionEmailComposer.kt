package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailBasis
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailItem
import com.sleekydz86.idolglow.subscription.application.event.IdolGlowIssueDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import com.sleekydz86.idolglow.subscription.application.ProductRecommendationMailLayout as Layout

@Component
class SubscriptionEmailComposer(
    private val appMailProperties: AppMailProperties,
    private val appPublicUrlProperties: AppPublicUrlProperties,
) {
    fun composeNewsletter(event: NewsletterDispatchRequestedEvent): ComposedSubscriptionEmail {
        val subject = "${appMailProperties.newsletterSubjectPrefix.trim()} ${event.title}".trim()
        val detailUrl = resolveUrl("/newsletters/${event.slug}")
        val tagLine = event.tags.take(6).joinToString(" ")
        val plainText =
            buildString {
                appendLine(event.title)
                appendLine("게시일: ${formatDate(event.publishedAt)}")
                appendLine()
                appendLine(event.summary)
                appendLine()
                event.paragraphs.take(5).forEach {
                    appendLine(it)
                    appendLine()
                }
                if (detailUrl != null) {
                    appendLine("자세히 보기: $detailUrl")
                }
            }.trim()

        val htmlBody =
            """
            <html>
              <body style="margin:0;padding:0;background:#f6f1e8;color:#1f1a18;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                <div style="max-width:680px;margin:0 auto;padding:32px 20px;">
                  <div style="background:#fffaf4;border-radius:24px;overflow:hidden;border:1px solid #eadfce;">
                    <div style="padding:28px;background:linear-gradient(135deg,#24343b,#c8673d);color:#fff7f1;">
                      <p style="margin:0 0 10px;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;">IdolGlow Newsletter</p>
                      <h1 style="margin:0;font-size:30px;line-height:1.2;">${escapeHtml(event.title)}</h1>
                      <p style="margin:14px 0 0;font-size:14px;opacity:0.86;">게시일 ${formatDate(event.publishedAt)}</p>
                    </div>
                    <div style="padding:28px;">
                      <img src="${escapeHtml(
                event.imageUrl,
            )}" alt="" style="width:100%;height:auto;border-radius:18px;display:block;object-fit:cover;max-height:360px;" />
                      <p style="margin:22px 0 0;font-size:16px;line-height:1.8;color:#544b46;">${escapeHtml(event.summary)}</p>
                      ${if (tagLine.isNotBlank()) {
                """<p style="margin:18px 0 0;font-size:13px;color:#8f5b43;">${escapeHtml(
                    tagLine,
                )}</p>"""
            } else {
                ""
            }}
                      ${event.paragraphs.take(5).joinToString("") { paragraph ->
                """<p style="margin:18px 0 0;font-size:15px;line-height:1.9;color:#2d2623;">${escapeHtml(paragraph)}</p>"""
            }}
                      ${if (detailUrl != null) {
                """
                        <div style="margin-top:28px;">
                          <a href="${escapeHtml(
                    detailUrl,
                )}" style="display:inline-block;padding:14px 20px;border-radius:999px;background:#cb5c33;color:#fffaf4;text-decoration:none;font-weight:700;">
                            뉴스레터 자세히 보기
                          </a>
                        </div>
                      """
            } else {
                ""
            }}
                    </div>
                  </div>
                </div>
              </body>
            </html>
            """.trimIndent()

        return ComposedSubscriptionEmail(subject = subject, plainText = plainText, htmlBody = htmlBody)
    }

    fun composeIdolGlowIssue(event: IdolGlowIssueDispatchRequestedEvent): ComposedSubscriptionEmail {
        val title = "Vol.${event.volume} 웹진이 도착했습니다"
        val subject = "${appMailProperties.idolglowSubjectPrefix.trim()} $title".trim()
        val detailUrl = resolveUrl("/issues/${event.slug}")
        val articleList = event.articleTitles.take(6)
        val plainText =
            buildString {
                appendLine(title)
                appendLine("발행일: ${formatDate(event.issueDate)}")
                appendLine()
                appendLine(event.teaser)
                if (articleList.isNotEmpty()) {
                    appendLine()
                    appendLine("이번 호 기사")
                    articleList.forEach { appendLine("- $it") }
                }
                if (detailUrl != null) {
                    appendLine()
                    appendLine("자세히 보기: $detailUrl")
                }
            }.trim()

        val htmlBody =
            """
            <html>
              <body style="margin:0;padding:0;background:#f3eee6;color:#201a18;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                <div style="max-width:680px;margin:0 auto;padding:32px 20px;">
                  <div style="background:#fffaf4;border-radius:24px;overflow:hidden;border:1px solid #e6d9c9;">
                    <div style="padding:28px;background:linear-gradient(135deg,#3d2b27,#b68b54);color:#fff8f0;">
                      <p style="margin:0 0 10px;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;">IdolGlow Issue</p>
                      <h1 style="margin:0;font-size:30px;line-height:1.2;">${escapeHtml(title)}</h1>
                      <p style="margin:14px 0 0;font-size:14px;opacity:0.86;">발행일 ${formatDate(event.issueDate)}</p>
                    </div>
                    <div style="padding:28px;">
                      <img src="${escapeHtml(
                event.coverImageUrl,
            )}" alt="" style="width:100%;height:auto;border-radius:18px;display:block;object-fit:cover;max-height:360px;" />
                      <p style="margin:22px 0 0;font-size:16px;line-height:1.8;color:#544b46;">${escapeHtml(event.teaser)}</p>
                      ${if (articleList.isNotEmpty()) {
                """
                        <div style="margin-top:24px;padding:18px;border-radius:18px;background:#f7efe2;">
                          <h2 style="margin:0 0 12px;font-size:18px;">이번 호 기사</h2>
                          ${articleList.joinToString(
                    "",
                ) { """<p style="margin:8px 0;font-size:15px;color:#2d2623;">• ${escapeHtml(it)}</p>""" }}
                        </div>
                      """
            } else {
                ""
            }}
                      ${if (detailUrl != null) {
                """
                        <div style="margin-top:28px;">
                          <a href="${escapeHtml(
                    detailUrl,
                )}" style="display:inline-block;padding:14px 20px;border-radius:999px;background:#24343b;color:#fffaf4;text-decoration:none;font-weight:700;">
                            웹진 바로 보기
                          </a>
                        </div>
                      """
            } else {
                ""
            }}
                    </div>
                  </div>
                </div>
              </body>
            </html>
            """.trimIndent()

        return ComposedSubscriptionEmail(subject = subject, plainText = plainText, htmlBody = htmlBody)
    }

    fun composeProductRecommendation(
        recipient: String,
        basis: ProductRecommendationMailBasis,
        items: List<ProductRecommendationMailItem>,
        unsubscribeUrl: String,
        surveyFallback: Boolean = false,
    ): ComposedSubscriptionEmail {
        val settings = appMailProperties.recommendation
        val title =
            when (basis) {
                ProductRecommendationMailBasis.PERSONALIZED -> "내 취향 추천 상품"
                ProductRecommendationMailBasis.POPULAR -> "이번 주 인기 상품"
            }
        val highlights = items.flatMap { it.highlights }.distinct().take(RECOMMENDATION_HIGHLIGHT_LIMIT)
        val basisLine =
            when {
                basis == ProductRecommendationMailBasis.POPULAR -> "추천 기준: 인기순"
                highlights.isEmpty() -> "추천 기준: 내 설문 취향"
                else -> "추천 기준: 내 설문 취향 (${highlights.joinToString(", ")})"
            }
        val content =
            RecommendationMailContent(
                title = title,
                recipient = recipient,
                notices = listOfNotNull(SURVEY_FALLBACK_NOTICE.takeIf { surveyFallback }, basisLine),
                items = items.take(RECOMMENDATION_ITEM_LIMIT),
                surveyUrl = resolveUrl("/my-survey", appPublicUrlProperties.frontendBaseUrl),
                unsubscribeUrl = unsubscribeUrl,
                senderName = settings.senderName.trim().ifEmpty { appMailProperties.fromName.trim() },
                senderContact =
                    settings.senderContact
                        .trim()
                        .ifEmpty { appMailProperties.replyTo.trim() }
                        .ifEmpty { appMailProperties.fromAddress.trim() },
            )
        val subject = listOf(settings.subjectPrefix.trim(), title).filter { it.isNotEmpty() }.joinToString(" ")
        return ComposedSubscriptionEmail(
            subject = "$ADVERTISING_MARK$subject",
            plainText = recommendationPlainText(content),
            htmlBody = recommendationHtml(content),
        )
    }

    private fun recommendationPlainText(content: RecommendationMailContent): String =
        buildString {
            appendLine("$ADVERTISING_MARK${content.title}")
            appendLine()
            content.notices.forEach(::appendLine)
            content.items.forEachIndexed { index, item ->
                appendLine()
                appendLine("${index + 1}. ${item.name}")
                appendLine("최저가 ${formatPrice(item.minPrice)}")
                item.reasons.take(RECOMMENDATION_REASON_LIMIT).forEach { appendLine("- $it") }
                productUrl(item)?.let { appendLine("상세 보기: $it") }
            }
            appendLine()
            appendLine(PRICE_NOTICE)
            content.surveyUrl?.let { appendLine("설문 수정: $it") }
            appendLine("수신 거부: ${content.unsubscribeUrl}")
            appendLine("보내는 곳: ${content.senderName} / 연락처: ${content.senderContact}")
            appendLine("이 메일은 ${content.recipient} 주소로 광고성 정보 수신에 동의하신 분께 보내 드립니다.")
        }.trim()

    private fun recommendationHtml(content: RecommendationMailContent): String {
        val body =
            buildString {
                append("<p style=\"margin:0 0 8px;font-size:12px;font-weight:700;color:${Layout.ACCENT};\">(광고)</p>")
                append(Layout.heading(escapeHtml(content.title)))
                append(Layout.highlight(content.notices.map(::escapeHtml)))
                content.items.forEach { append(productCardHtml(it)) }
                append("<p style=\"margin:24px 0 0;font-size:13px;line-height:1.7;color:${Layout.SUBTLE};\">")
                append("${escapeHtml(PRICE_NOTICE)}</p>")
                content.surveyUrl?.let {
                    append("<p style=\"margin:10px 0 0;font-size:13px;\">")
                    append("${Layout.link(escapeHtml(it), "설문 수정하기")}</p>")
                }
            }
        val footer =
            Layout.footnote(
                "보내는 곳: ${escapeHtml(content.senderName)} · 연락처: ${escapeHtml(content.senderContact)}<br/>" +
                    "이 메일은 ${escapeHtml(content.recipient)} 주소로 광고성 정보 수신에 동의하신 분께 보내 드립니다.<br/>" +
                    "더 이상 받지 않으려면 ${Layout.link(escapeHtml(content.unsubscribeUrl), "수신 거부")}를 눌러 주세요.",
            )
        return Layout.document(body = body, footer = footer)
    }

    private fun productCardHtml(item: ProductRecommendationMailItem): String =
        buildString {
            append("<table ${Layout.TABLE_ATTRS} width=\"100%\" style=\"margin:16px 0 0;border-collapse:collapse;")
            append("border:1px solid ${Layout.FRAME};border-top:2px solid ${Layout.INK};\"><tr>")
            append("<td style=\"padding:16px 18px;background:#ffffff;\">")
            append("<p style=\"margin:0;font-size:17px;font-weight:700;line-height:1.45;color:${Layout.INK};\">")
            append("${escapeHtml(item.name)}</p>")
            append("<p style=\"margin:6px 0 0;font-size:14px;color:${Layout.BODY};\">")
            append("최저가 <strong style=\"color:${Layout.INK};\">${escapeHtml(formatPrice(item.minPrice))}</strong></p>")
            val reasons = item.reasons.take(RECOMMENDATION_REASON_LIMIT)
            if (reasons.isNotEmpty()) {
                append("<div style=\"margin:12px 0 0;padding:2px 0 2px 10px;border-left:3px solid ${Layout.ACCENT};\">")
                reasons.forEach {
                    append("<p style=\"margin:2px 0;font-size:13px;line-height:1.6;color:${Layout.BODY};\">")
                    append("· ${escapeHtml(it)}</p>")
                }
                append("</div>")
            }
            productUrl(item)?.let { append(Layout.button(escapeHtml(it), "상세 보기", marginTop = 14)) }
            append("</td></tr></table>")
        }

    private fun productUrl(item: ProductRecommendationMailItem): String? =
        resolveUrl("/products/${item.productId}", appPublicUrlProperties.frontendBaseUrl)

    private fun resolveUrl(
        path: String,
        baseUrl: String = appPublicUrlProperties.publicBaseUrl,
    ): String? {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isBlank()) return null
        return "$base$path"
    }

    private fun formatDate(date: LocalDate): String = requireNotNull(IdolGlowDateTimes.toDisplay(date))

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br/>")

    private data class RecommendationMailContent(
        val title: String,
        val recipient: String,
        val notices: List<String>,
        val items: List<ProductRecommendationMailItem>,
        val surveyUrl: String?,
        val unsubscribeUrl: String,
        val senderName: String,
        val senderContact: String,
    )

    companion object {
        private const val ADVERTISING_MARK = "(광고) "
        private const val RECOMMENDATION_ITEM_LIMIT = 5
        private const val RECOMMENDATION_REASON_LIMIT = 2
        private const val RECOMMENDATION_HIGHLIGHT_LIMIT = 3
        private const val SURVEY_FALLBACK_NOTICE = "설문과 맞는 상품이 없어 인기 상품을 보내 드려요."
        private const val PRICE_NOTICE = "가격은 발송 시점 기준이며, 예약 가능 여부는 상세 화면에서 확인해 주세요."

        private fun formatPrice(price: BigDecimal): String {
            val won = DecimalFormat("#,##0").format(price.setScale(0, RoundingMode.HALF_UP))
            return "${won}원"
        }
    }
}
