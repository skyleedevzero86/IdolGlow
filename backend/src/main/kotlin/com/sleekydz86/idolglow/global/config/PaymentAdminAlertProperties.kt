package com.sleekydz86.idolglow.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.admin-alert")
data class PaymentAdminAlertProperties(
    var discordWebhookUrl: String = "",
    var telegramBotToken: String = "",
    var telegramChatId: String = "",
)
