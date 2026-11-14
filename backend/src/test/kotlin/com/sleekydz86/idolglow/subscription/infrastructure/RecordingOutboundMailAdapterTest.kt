package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.`in`.MailSendLogRecorder
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("메일 발송 기록 데코레이터")
class RecordingOutboundMailAdapterTest {
    private val transport = mockk<OutboundMailTransport>()
    private val recorder = mockk<MailSendLogRecorder>()
    private val adapter = RecordingOutboundMailAdapter(transport, recorder)
    private val message =
        OutboundMailMessage(
            to = "reader@idolglow.test",
            subject = "제목",
            plainTextBody = "본문",
            htmlBody = "<p>본문</p>",
            kind = MailSendKind.NEWSLETTER,
            dispatchId = 7L,
        )

    @DisplayName("보내기에 성공하면 SENT 로 기록한다")
    @Test
    fun recordsSentAfterDelivery() {
        // given
        every { transport.send(message) } just runs
        every { recorder.record(any(), any(), any()) } just runs

        // when
        adapter.send(message)

        // then
        verify(exactly = 1) { transport.send(message) }
        verify(exactly = 1) { recorder.record(message, MailSendStatus.SENT, null) }
    }

    @DisplayName("보내기에 실패하면 FAILED 와 원인을 기록하고 예외를 그대로 던진다")
    @Test
    fun recordsFailedAndRethrows() {
        // given
        val failure = IllegalStateException("SMTP 연결 실패")
        every { transport.send(message) } throws failure
        every { recorder.record(any(), any(), any()) } just runs

        // when
        val thrown = assertThrows(IllegalStateException::class.java) { adapter.send(message) }

        // then
        assertEquals(failure, thrown)
        verify(exactly = 1) { recorder.record(message, MailSendStatus.FAILED, "SMTP 연결 실패") }
    }

    @DisplayName("기록 저장이 실패해도 메일 발송은 성공으로 끝난다")
    @Test
    fun loggingFailureDoesNotBreakSending() {
        // given
        every { transport.send(message) } just runs
        every { recorder.record(any(), any(), any()) } throws IllegalStateException("DB 오류")

        // when
        adapter.send(message)

        // then
        verify(exactly = 1) { transport.send(message) }
        verify(exactly = 1) { recorder.record(message, MailSendStatus.SENT, null) }
    }

    @DisplayName("기록 저장이 실패해도 발송 실패 예외는 원래 예외로 전달된다")
    @Test
    fun loggingFailureKeepsOriginalSendFailure() {
        // given
        val failure = IllegalStateException("SMTP 거절")
        every { transport.send(message) } throws failure
        every { recorder.record(any(), any(), any()) } throws IllegalStateException("DB 오류")

        // when
        val thrown = assertThrows(IllegalStateException::class.java) { adapter.send(message) }

        // then
        assertEquals(failure, thrown)
    }
}
