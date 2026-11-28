package com.sleekydz86.idolglow.global.infrastructure.exception.billing

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class BillingPlanChangeExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    SUBSCRIPTION_NOT_FOUND(
        "PLAN_CHANGE_SUBSCRIPTION_NOT_FOUND",
        "이용 중인 Glow Pass 구독이 없습니다.",
        NOT_FOUND,
    ),
    INVALID_CYCLE(
        "PLAN_CHANGE_INVALID_CYCLE",
        "바꿀 수 있는 이용권은 3개월, 6개월, 1년입니다.",
        BAD_REQUEST,
    ),
    SAME_CYCLE(
        "PLAN_CHANGE_SAME_CYCLE",
        "지금 이용 중인 이용권과 같은 이용권입니다.",
        BAD_REQUEST,
    ),
    NOT_ALLOWED(
        "PLAN_CHANGE_NOT_ALLOWED",
        "자동 연장 중이고 결제 실패가 없는 구독만 이용권을 바꿀 수 있습니다.",
        CONFLICT,
    ),
    NOT_CHARGED_YET(
        "PLAN_CHANGE_NOT_CHARGED_YET",
        "첫 결제 전에는 이용권을 바꿀 수 없습니다.",
        CONFLICT,
    ),
    TOO_CLOSE(
        "PLAN_CHANGE_TOO_CLOSE",
        "다음 결제일 하루 전과 당일에는 이용권을 바꿀 수 없습니다.",
        CONFLICT,
    ),
    CHARGE_PENDING(
        "PLAN_CHANGE_CHARGE_PENDING",
        "결과를 확인 중인 결제가 있어 지금은 이용권을 바꿀 수 없습니다.",
        CONFLICT,
    ),
    AMOUNT_CHANGED(
        "PLAN_CHANGE_AMOUNT_CHANGED",
        "변경 금액이 바뀌었습니다. 금액을 다시 확인해 주세요.",
        CONFLICT,
    ),
    METHOD_UNUSABLE(
        "PLAN_CHANGE_METHOD_UNUSABLE",
        "기본 카드로 결제할 수 없습니다. 카드를 다시 등록해 주세요.",
        CONFLICT,
    ),
    PAYMENT_FAILED(
        "PLAN_CHANGE_PAYMENT_FAILED",
        "차액 결제에 실패해 이용권을 바꾸지 않았습니다.",
        PAYMENT_REQUIRED,
    ),
}

private const val BAD_REQUEST = 400
private const val PAYMENT_REQUIRED = 402
private const val NOT_FOUND = 404
private const val CONFLICT = 409
