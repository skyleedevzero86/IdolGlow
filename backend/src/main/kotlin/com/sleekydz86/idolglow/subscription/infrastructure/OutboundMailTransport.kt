package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage

interface OutboundMailTransport {
    fun send(message: OutboundMailMessage)
}
