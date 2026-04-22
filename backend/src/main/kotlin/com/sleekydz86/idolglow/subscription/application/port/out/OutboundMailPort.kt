package com.sleekydz86.idolglow.subscription.application.port.out

data class OutboundMailMessage(
    val to: String,
    val subject: String,
    val plainTextBody: String,
    val htmlBody: String,
)

interface OutboundMailPort {
    fun send(message: OutboundMailMessage)
}
