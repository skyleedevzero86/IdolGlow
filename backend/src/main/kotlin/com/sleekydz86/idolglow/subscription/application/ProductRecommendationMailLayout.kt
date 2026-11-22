package com.sleekydz86.idolglow.subscription.application

internal object ProductRecommendationMailLayout {
    const val INK = "#111111"
    const val BODY = "#333333"
    const val SUBTLE = "#555555"
    const val LINE = "#eeeeee"
    const val FRAME = "#e8e8e8"
    const val SOFT_BG = "#f7f7f5"
    const val BAR = "#0d0d0d"
    const val ACCENT = "#e31c2a"
    const val TABLE_ATTRS = "role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""

    private const val FONT = "Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif"
    private const val WORDMARK = "IdolGlow"

    fun document(
        body: String,
        footer: String? = null,
    ): String =
        buildString {
            append("<html><head><meta charset=\"UTF-8\" />")
            append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" /></head>")
            append("<body style=\"margin:0;padding:0;background:#ffffff;color:$BODY;font-family:$FONT;\">")
            append("<table $TABLE_ATTRS width=\"100%\" style=\"background:#ffffff;\"><tr>")
            append("<td align=\"center\" style=\"padding:32px 12px;\">")
            append("<table $TABLE_ATTRS width=\"600\" style=\"width:100%;max-width:600px;background:#ffffff;")
            append("border:1px solid $FRAME;border-collapse:collapse;text-align:left;\">")
            append("<tr><td style=\"padding:16px 28px;background:$BAR;border-bottom:3px solid $ACCENT;\">")
            append("<span style=\"color:#ffffff;font-size:18px;font-weight:800;letter-spacing:-0.02em;\">")
            append("$WORDMARK</span></td></tr>")
            append("<tr><td style=\"padding:28px;color:$BODY;font-family:$FONT;\">")
            append(body)
            append("</td></tr>")
            footer?.let {
                append("<tr><td style=\"padding:18px 28px;background:$SOFT_BG;border-top:1px solid $LINE;")
                append("font-family:$FONT;\">")
                append(it)
                append("</td></tr>")
            }
            append("</table></td></tr></table></body></html>")
        }

    fun heading(textHtml: String): String =
        "<h1 style=\"margin:0;font-size:22px;line-height:1.4;font-weight:800;color:$INK;" +
            "letter-spacing:-0.02em;\">$textHtml</h1>"

    fun paragraph(textHtml: String): String =
        "<p style=\"margin:10px 0 0;font-size:14px;line-height:1.8;color:$BODY;\">$textHtml</p>"

    fun highlight(linesHtml: List<String>): String =
        buildString {
            append("<div style=\"margin:16px 0 0;padding:10px 14px;border-left:3px solid $ACCENT;")
            append("background:$SOFT_BG;\">")
            linesHtml.forEach {
                append("<p style=\"margin:2px 0;font-size:14px;line-height:1.7;color:$INK;\">$it</p>")
            }
            append("</div>")
        }

    fun button(
        hrefHtml: String,
        labelHtml: String,
        marginTop: Int = 20,
    ): String =
        "<table $TABLE_ATTRS style=\"margin:${marginTop}px 0 0;border-collapse:collapse;\"><tr>" +
            "<td style=\"background:$ACCENT;\"><a href=\"$hrefHtml\" style=\"display:inline-block;" +
            "padding:11px 22px;color:#ffffff;font-size:14px;font-weight:700;line-height:1.2;" +
            "text-decoration:none;\">$labelHtml</a></td></tr></table>"

    fun link(
        hrefHtml: String,
        labelHtml: String,
    ): String = "<a href=\"$hrefHtml\" style=\"color:$ACCENT;font-weight:700;text-decoration:underline;\">$labelHtml</a>"

    fun footnote(textHtml: String): String =
        "<p style=\"margin:0;font-size:12px;line-height:1.8;color:$SUBTLE;\">$textHtml</p>"
}
