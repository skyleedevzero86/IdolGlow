package com.sleekydz86.idolglow.survey.domain.autopick

enum class SurveyAutoPickFailReason {
    DISABLED,
    NOT_MEMBER,
    NO_BILLING_METHOD,
    OVER_BUDGET,
    MONTHLY_LIMIT,
    SLOT_UNAVAILABLE,
    PAYMENT_FAILED,
}
