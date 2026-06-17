package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.infrastructure.config.AppMailProperties
import com.sleekydz86.idolglow.global.infrastructure.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.WebzineIssueDispatchRequestedEvent
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ComposedSubscriptionEmail(
    val subject: String,
    val plainText: String,
    val htmlBody: String,
)
