package com.sleekydz86.idolglow.subscription.domain

enum class SubscriptionDispatchStatus(val label: String) {
    RECORDED("기록됨"),
    SENT("발송완료"),
    FAILED("발송실패"),
}
