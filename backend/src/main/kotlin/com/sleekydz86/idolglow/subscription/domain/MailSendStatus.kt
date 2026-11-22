package com.sleekydz86.idolglow.subscription.domain

enum class MailSendStatus(
    val label: String,
) {
    SENT("발송"),
    FAILED("실패"),
    SKIPPED("건너뜀"),
}
