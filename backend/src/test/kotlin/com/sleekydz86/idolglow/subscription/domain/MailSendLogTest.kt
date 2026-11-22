package com.sleekydz86.idolglow.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("메일 발송 기록")
class MailSendLogTest {
    private val now = LocalDateTime.of(2026, 11, 14, 10, 0)

    @DisplayName("본문의 수신 거부 링크 토큰은 token=*** 로 가린다")
    @Test
    fun masksUnsubscribeTokens() {
        // given
        val html =
            """<a href="https://idolglow.test/recommendation-mail/unsubscribe?token=abc.DEF-123_x">거부</a>""" +
                """<a href="https://idolglow.test/x?a=1&amp;token=secret&amp;b=2">x</a>"""
        val text = "수신 거부: https://idolglow.test/recommendation-mail/unsubscribe?token=abc.DEF-123_x\n끝"

        // when
        val log = record(MailSendKind.PRODUCT_RECOMMENDATION, html, text, MailSendStatus.SENT, null)

        // then
        assertEquals(
            """<a href="https://idolglow.test/recommendation-mail/unsubscribe?token=***">거부</a>""" +
                """<a href="https://idolglow.test/x?a=1&amp;token=***&amp;b=2">x</a>""",
            log.htmlBody,
        )
        assertEquals("수신 거부: https://idolglow.test/recommendation-mail/unsubscribe?token=***\n끝", log.textBody)
        assertEquals(now, log.sentAt)
    }

    @DisplayName("인증 메일은 임시 비밀번호가 남지 않도록 본문을 저장하지 않는다")
    @Test
    fun authMailBodyIsNotStored() {
        // given
        val html = "<p>임시 비밀번호: Abcd1234!</p>"

        // when
        val log = record(MailSendKind.AUTH, html, "임시 비밀번호: Abcd1234!", MailSendStatus.SENT, null)

        // then
        assertEquals(MailSendLog.AUTH_BODY_NOTICE, log.htmlBody)
        assertEquals(MailSendLog.AUTH_BODY_NOTICE, log.textBody)
        assertFalse(log.htmlBody.orEmpty().contains("Abcd1234"))
    }

    @DisplayName("실패 기록은 발송 시각이 없고 원인은 500자로 자르며 토큰을 가린다")
    @Test
    fun failedLogKeepsTrimmedError() {
        // given
        val error = "token=leak " + "x".repeat(600)

        // when
        val log = record(MailSendKind.NEWSLETTER, "<p>본문</p>", "본문", MailSendStatus.FAILED, error)

        // then
        assertNull(log.sentAt)
        assertEquals(MailSendLog.ERROR_MAX_LENGTH, log.error?.length)
        assertTrue(log.error.orEmpty().startsWith("token=*** "))
        assertEquals(now, log.createdAt)
    }

    private fun record(
        kind: MailSendKind,
        html: String,
        text: String,
        status: MailSendStatus,
        error: String?,
    ): MailSendLog =
        MailSendLog.record(
            kind = kind,
            recipientEmail = " reader@idolglow.test ",
            subject = "제목",
            htmlBody = html,
            textBody = text,
            status = status,
            error = error,
            dispatchId = null,
            now = now,
        )
}
