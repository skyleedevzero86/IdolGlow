package com.sleekydz86.idolglow.payment.domain

enum class PlanChangeBlock {
    NOT_ACTIVE,
    HAS_FAILURE,
    NOT_CHARGED_YET,
    SAME_CYCLE,
    TOO_CLOSE,
}
