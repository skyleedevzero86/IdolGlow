package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus

interface MailSendLogRecorder {
    fun record(
        message: OutboundMailMessage,
        status: MailSendStatus,
        error: String?,
    )
}
