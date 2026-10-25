package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.global.config.PaymentAdminAlertProperties
import com.sleekydz86.idolglow.payment.application.StuckPaymentAdminNotifier
import com.sleekydz86.idolglow.payment.application.StuckPaymentAlert
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class WebhookStuckPaymentAdminNotifier(
    private val properties: PaymentAdminAlertProperties,
) : StuckPaymentAdminNotifier {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient: RestClient = RestClient.create()

    override fun notify(alert: StuckPaymentAlert): Boolean {
        val text = alert.describe()
        val discordSent = sendDiscord(text)
        val telegramSent = sendTelegram(text)
        return discordSent || telegramSent
    }

    private fun sendDiscord(text: String): Boolean {
        val url = properties.discordWebhookUrl.trim()
        if (!url.startsWith("https://")) {
            return false
        }
        return post(url, """{"content":"${escape(text)}"}""")
    }

    private fun sendTelegram(text: String): Boolean {
        val token = properties.telegramBotToken.trim()
        val chatId = properties.telegramChatId.trim()
        if (token.isEmpty() || chatId.isEmpty()) {
            return false
        }
        val body = """{"chat_id":"${escape(chatId)}","text":"${escape(text)}"}"""
        return post("https://api.telegram.org/bot$token/sendMessage", body)
    }

    private fun post(
        url: String,
        json: String,
    ): Boolean =
        runCatching {
            restClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .toBodilessEntity()
                .statusCode
                .is2xxSuccessful
        }.onFailure { log.warn("관리자 알림 웹훅 전송에 실패했습니다.") }
            .getOrDefault(false)

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n")
}
