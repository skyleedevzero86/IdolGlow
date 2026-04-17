package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.WebzineIssueDispatchRequestedEvent
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class SubscriptionEmailComposer(
    private val appMailProperties: AppMailProperties,
    private val appPublicUrlProperties: AppPublicUrlProperties,
) {

    fun composeNewsletter(event: NewsletterDispatchRequestedEvent): ComposedSubscriptionEmail {
        val subject = "${appMailProperties.newsletterSubjectPrefix.trim()} ${event.title}".trim()
        val detailUrl = resolveUrl("/newsletters/${event.slug}")
        val tagLine = event.tags.take(6).joinToString(" ")
        val plainText = buildString {
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

        val htmlBody = """
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
                      <img src="${escapeHtml(event.imageUrl)}" alt="" style="width:100%;height:auto;border-radius:18px;display:block;object-fit:cover;max-height:360px;" />
                      <p style="margin:22px 0 0;font-size:16px;line-height:1.8;color:#544b46;">${escapeHtml(event.summary)}</p>
                      ${if (tagLine.isNotBlank()) """<p style="margin:18px 0 0;font-size:13px;color:#8f5b43;">${escapeHtml(tagLine)}</p>""" else ""}
                      ${event.paragraphs.take(5).joinToString("") { paragraph ->
            """<p style="margin:18px 0 0;font-size:15px;line-height:1.9;color:#2d2623;">${escapeHtml(paragraph)}</p>"""
        }}
                      ${if (detailUrl != null) """
                        <div style="margin-top:28px;">
                          <a href="${escapeHtml(detailUrl)}" style="display:inline-block;padding:14px 20px;border-radius:999px;background:#cb5c33;color:#fffaf4;text-decoration:none;font-weight:700;">
                            뉴스레터 자세히 보기
                          </a>
                        </div>
                      """ else ""}
                    </div>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        return ComposedSubscriptionEmail(subject = subject, plainText = plainText, htmlBody = htmlBody)
    }

    fun composeWebzineIssue(event: WebzineIssueDispatchRequestedEvent): ComposedSubscriptionEmail {
        val title = "Vol.${event.volume} 웹진이 도착했습니다"
        val subject = "${appMailProperties.webzineSubjectPrefix.trim()} $title".trim()
        val detailUrl = resolveUrl("/issues/${event.slug}")
        val articleList = event.articleTitles.take(6)
        val plainText = buildString {
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

        val htmlBody = """
            <html>
              <body style="margin:0;padding:0;background:#f3eee6;color:#201a18;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;">
                <div style="max-width:680px;margin:0 auto;padding:32px 20px;">
                  <div style="background:#fffaf4;border-radius:24px;overflow:hidden;border:1px solid #e6d9c9;">
                    <div style="padding:28px;background:linear-gradient(135deg,#3d2b27,#b68b54);color:#fff8f0;">
                      <p style="margin:0 0 10px;font-size:12px;letter-spacing:0.18em;text-transform:uppercase;">IdolGlow Webzine</p>
                      <h1 style="margin:0;font-size:30px;line-height:1.2;">${escapeHtml(title)}</h1>
                      <p style="margin:14px 0 0;font-size:14px;opacity:0.86;">발행일 ${formatDate(event.issueDate)}</p>
                    </div>
                    <div style="padding:28px;">
                      <img src="${escapeHtml(event.coverImageUrl)}" alt="" style="width:100%;height:auto;border-radius:18px;display:block;object-fit:cover;max-height:360px;" />
                      <p style="margin:22px 0 0;font-size:16px;line-height:1.8;color:#544b46;">${escapeHtml(event.teaser)}</p>
                      ${if (articleList.isNotEmpty()) """
                        <div style="margin-top:24px;padding:18px;border-radius:18px;background:#f7efe2;">
                          <h2 style="margin:0 0 12px;font-size:18px;">이번 호 기사</h2>
                          ${articleList.joinToString("") { """<p style="margin:8px 0;font-size:15px;color:#2d2623;">• ${escapeHtml(it)}</p>""" }}
                        </div>
                      """ else ""}
                      ${if (detailUrl != null) """
                        <div style="margin-top:28px;">
                          <a href="${escapeHtml(detailUrl)}" style="display:inline-block;padding:14px 20px;border-radius:999px;background:#24343b;color:#fffaf4;text-decoration:none;font-weight:700;">
                            웹진 바로 보기
                          </a>
                        </div>
                      """ else ""}
                    </div>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        return ComposedSubscriptionEmail(subject = subject, plainText = plainText, htmlBody = htmlBody)
    }

    private fun resolveUrl(path: String): String? {
        val base = appPublicUrlProperties.publicBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return null
        return "$base$path"
    }

    private fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br/>")

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    }
}

data class ComposedSubscriptionEmail(
    val subject: String,
    val plainText: String,
    val htmlBody: String,
)
