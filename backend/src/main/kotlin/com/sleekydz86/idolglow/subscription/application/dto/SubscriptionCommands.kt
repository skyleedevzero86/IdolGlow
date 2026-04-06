package com.sleekydz86.idolglow.subscription.application.dto

data class RegisterSubscriptionCommand(
    val email: String,
    val subscribeNewsletters: Boolean,
    val subscribeIssues: Boolean,
    val source: String,
)
