package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.global.infrastructure.config.AppMailProperties
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import jakarta.mail.internet.InternetAddress
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class SmtpOutboundMailAdapter(
    private val javaMailSender: JavaMailSender,
    private val appMailProperties: AppMailProperties,
) : OutboundMailPort {

    override fun send(message: OutboundMailMessage) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, "UTF-8")

        helper.setTo(message.to)
        helper.setSubject(message.subject)
        helper.setText(message.plainTextBody, message.htmlBody)
        helper.setFrom(InternetAddress(appMailProperties.fromAddress, appMailProperties.fromName))
        appMailProperties.replyTo.trim().takeIf { it.isNotEmpty() }?.let(helper::setReplyTo)

        javaMailSender.send(mimeMessage)
    }
}
