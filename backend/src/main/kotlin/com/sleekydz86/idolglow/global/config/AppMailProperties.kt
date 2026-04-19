package com.sleekydz86.idolglow.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.mail")
data class AppMailProperties(
    var enabled: Boolean = false,
    var fromAddress: String = "no-reply@idolglow.app",
    var fromName: String = "IdolGlow",
    var replyTo: String = "",
    var newsletterSubjectPrefix: String = "[IdolGlow Newsletter]",
    var webzineSubjectPrefix: String = "[IdolGlow Webzine]",
    var smtp: Smtp = Smtp(),
) {
    data class Smtp(
        var host: String = "",
        var port: Int = 587,
        var username: String = "",
        var password: String = "",
        var auth: Boolean = true,
        var starttlsEnabled: Boolean = true,
        var connectionTimeoutMs: Long = 5000,
        var timeoutMs: Long = 10000,
        var writeTimeoutMs: Long = 10000,
    )
}
