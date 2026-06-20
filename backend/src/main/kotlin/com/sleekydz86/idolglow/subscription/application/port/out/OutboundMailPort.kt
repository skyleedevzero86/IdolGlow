package com.sleekydz86.idolglow.subscription.application.port.out

interface OutboundMailPort {
    fun send(message: OutboundMailMessage)
}
