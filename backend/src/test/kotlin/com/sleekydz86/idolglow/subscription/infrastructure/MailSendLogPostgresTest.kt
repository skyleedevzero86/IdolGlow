package com.sleekydz86.idolglow.subscription.infrastructure

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.subscription.application.MailSendLogRetentionScheduler
import com.sleekydz86.idolglow.subscription.application.MailSendLogTxService
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.application.port.out.MailSendLogPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailSendException
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("메일 발송 기록 저장과 보관 기간")
class MailSendLogPostgresTest : PostgresIntegrationTest() {
    @MockkBean
    private lateinit var outboundMailTransport: OutboundMailTransport

    @Autowired
    private lateinit var outboundMailPort: OutboundMailPort

    @Autowired
    private lateinit var mailSendLogPort: MailSendLogPort

    @Autowired
    private lateinit var mailSendLogTxService: MailSendLogTxService

    @DisplayName("발송 포트로 보낸 메일은 성공·실패 모두 기록되고 본문 토큰은 가려진다")
    @Test
    fun recordsSentAndFailedMailsWithMaskedTokens() {
        // given
        val okEmail = "ok${token()}@idolglow.test"
        val failEmail = "fail${token()}@idolglow.test"
        every { outboundMailTransport.send(match { it.to == okEmail }) } just runs
        every { outboundMailTransport.send(match { it.to == failEmail }) } throws MailSendException("수신 서버 거절")

        // when
        outboundMailPort.send(message(okEmail, dispatchId = null))
        val thrown =
            assertThrows(MailSendException::class.java) { outboundMailPort.send(message(failEmail, dispatchId = null)) }

        // then
        val sent = findByRecipient(okEmail)
        val failed = findByRecipient(failEmail)
        assertEquals("수신 서버 거절", thrown.message)
        assertEquals(MailSendStatus.SENT, sent.status)
        assertEquals(MailSendKind.PRODUCT_RECOMMENDATION, sent.mailKind)
        assertNotNull(sent.sentAt)
        assertTrue(sent.htmlBody.orEmpty().contains("unsubscribe?token=***"))
        assertFalse(sent.htmlBody.orEmpty().contains("raw-secret"))
        assertFalse(sent.textBody.orEmpty().contains("raw-secret"))
        assertEquals(MailSendStatus.FAILED, failed.status)
        assertEquals("수신 서버 거절", failed.error)
        assertNull(failed.sentAt)
    }

    @DisplayName("보관 기간 정리는 90일이 지난 기록만 지운다")
    @Test
    fun retentionDeletesOnlyExpiredLogs() {
        // given
        val now = LocalDateTime.now(IdolGlowDateTimes.ZONE).withNano(0)
        val retentionScheduler =
            MailSendLogRetentionScheduler(mailSendLogTxService, RETENTION_DAYS).also {
                it.clock = Clock.fixed(now.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
            }
        val expired = save("expired${token()}@idolglow.test", now.minusDays(91))
        val boundary = save("boundary${token()}@idolglow.test", now.minusDays(90).plusMinutes(1))
        val recent = save("recent${token()}@idolglow.test", now.minusDays(1))

        // when
        retentionScheduler.purgeExpired()

        // then
        assertNull(mailSendLogPort.findById(expired.id))
        assertNotNull(mailSendLogPort.findById(boundary.id))
        assertNotNull(mailSendLogPort.findById(recent.id))
    }

    private fun findByRecipient(email: String): MailSendLog =
        mailSendLogPort
            .search(MailSendLogSearchCondition(keyword = email), OffsetPageRequest.of(0, 10))
            .items
            .single()

    private fun save(
        email: String,
        createdAt: LocalDateTime,
    ): MailSendLog =
        mailSendLogPort.save(
            MailSendLog.record(
                kind = MailSendKind.NEWSLETTER,
                recipientEmail = email,
                subject = "보관 기간",
                htmlBody = "<p>본문</p>",
                textBody = "본문",
                status = MailSendStatus.SENT,
                error = null,
                dispatchId = null,
                now = createdAt,
            ),
        )

    private fun message(
        to: String,
        dispatchId: Long?,
    ): OutboundMailMessage =
        OutboundMailMessage(
            to = to,
            subject = "이번 주 추천",
            plainTextBody = "수신 거부: https://idolglow.test/recommendation-mail/unsubscribe?token=raw-secret",
            htmlBody = """<a href="https://idolglow.test/recommendation-mail/unsubscribe?token=raw-secret">수신 거부</a>""",
            kind = MailSendKind.PRODUCT_RECOMMENDATION,
            dispatchId = dispatchId,
        )

    private fun token(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(10)

    companion object {
        private const val RETENTION_DAYS = 90L
    }
}
