package com.sleekydz86.idolglow.payment.application.dto

data class TossBillingClientConfigResponse(
    val clientKey: String,
    val customerKey: String,
    val successUrl: String,
    val failUrl: String,
)
