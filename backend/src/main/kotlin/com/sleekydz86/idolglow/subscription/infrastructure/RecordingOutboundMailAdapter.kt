package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.`in`.MailSendLogRecorder
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RecordingOutboundMailAdapter(
    private val outboundMailTransport: OutboundMailTransport,
    private val mailSendLogRecorder: MailSendLogRecorder,
) : OutboundMailPort {
    private val log = LoggerFactory.getLogger(RecordingOutboundMailAdapter::class.java)

    override fun send(message: OutboundMailMessage) {
        val result = runCatching { outboundMailTransport.send(message) }
        val failure = result.exceptionOrNull()
        if (failure == null) {
            record(message, MailSendStatus.SENT, null)
        } else {
            record(message, MailSendStatus.FAILED, failure.message ?: failure.javaClass.simpleName)
        }
        result.getOrThrow()
    }

    private fun record(
        message: OutboundMailMessage,
        status: MailSendStatus,
        error: String?,
    ) {
        runCatching { mailSendLogRecorder.record(message, status, error) }
            .onFailure { ex ->
                log.warn(
                    "메일 발송 기록 저장 실패: kind={} status={} cause={}",
                    message.kind,
                    status,
                    ex.javaClass.simpleName,
                )
            }
    }
}
