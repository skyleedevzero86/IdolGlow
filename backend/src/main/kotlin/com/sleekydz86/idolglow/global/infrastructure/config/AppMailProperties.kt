package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.mail")
data class AppMailProperties(
    var enabled: Boolean = true,
    var fromAddress: String = "no-reply@idolglow.app",
    var fromName: String = "IdolGlow",
    var replyTo: String = "",
    var newsletterSubjectPrefix: String = "[IdolGlow Newsletter]",
    var webzineSubjectPrefix: String = "[IdolGlow Webzine]",
)
