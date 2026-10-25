package com.sleekydz86.idolglow.subscription.domain

enum class SubscriptionDispatchStatus(
    val label: String,
) {
    RECORDED("기록됨"),
    SENDING("발송중"),
    SENT("발송완료"),
    PARTIAL_FAILED("일부실패"),
    FAILED("발송실패"),
}
