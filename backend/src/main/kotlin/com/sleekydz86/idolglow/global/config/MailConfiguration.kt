package com.sleekydz86.idolglow.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class MailConfiguration(
    private val appMailProperties: AppMailProperties,
) {

    @Bean
    fun javaMailSender(): JavaMailSender {
        val smtp = appMailProperties.smtp
        return JavaMailSenderImpl().apply {
            host = smtp.host
            port = smtp.port
            username = smtp.username
            password = smtp.password
            javaMailProperties = javaMailProperties.apply {
                put("mail.smtp.auth", smtp.auth.toString())
                put("mail.smtp.starttls.enable", smtp.starttlsEnabled.toString())
                put("mail.smtp.connectiontimeout", smtp.connectionTimeoutMs.toString())
                put("mail.smtp.timeout", smtp.timeoutMs.toString())
                put("mail.smtp.writetimeout", smtp.writeTimeoutMs.toString())
            }
        }
    }
}
