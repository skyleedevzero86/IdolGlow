package com.sleekydz86.idolglow.subscription.application

data class ComposedSubscriptionEmail(
    val subject: String,
    val plainText: String,
    val htmlBody: String,
)
