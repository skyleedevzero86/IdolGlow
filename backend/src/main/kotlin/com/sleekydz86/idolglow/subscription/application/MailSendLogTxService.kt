package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.port.`in`.MailSendLogRecorder
import com.sleekydz86.idolglow.subscription.application.port.out.MailSendLogPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MailSendLogTxService(
    private val mailSendLogPort: MailSendLogPort,
) : MailSendLogRecorder {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun record(
        message: OutboundMailMessage,
        status: MailSendStatus,
        error: String?,
    ) {
        mailSendLogPort.save(
            MailSendLog.record(
                kind = message.kind,
                recipientEmail = message.to,
                subject = message.subject,
                htmlBody = message.htmlBody,
                textBody = message.plainTextBody,
                status = status,
                error = error,
                dispatchId = message.dispatchId,
                now = LocalDateTime.now(),
            ),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun deleteCreatedBefore(before: LocalDateTime): Int = mailSendLogPort.deleteCreatedBefore(before)
}
