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
)
