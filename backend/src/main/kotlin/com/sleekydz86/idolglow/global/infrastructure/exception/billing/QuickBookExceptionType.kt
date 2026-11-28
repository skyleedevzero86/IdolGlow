package com.sleekydz86.idolglow.global.infrastructure.exception.billing

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class QuickBookExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    BILLING_METHOD_REQUIRED(
        "BILLING_METHOD_REQUIRED",
        "기본 카드가 없습니다. 카드를 먼저 등록해 주세요.",
        BAD_REQUEST,
    ),
    PAYMENT_FAILED(
        "QUICK_BOOK_PAYMENT_FAILED",
        "기본 카드 결제에 실패해 예약을 취소했습니다.",
        PAYMENT_REQUIRED,
    ),
    QUICK_CANCEL_EXPIRED(
        "QUICK_CANCEL_EXPIRED",
        "바로 취소할 수 있는 시간이 지났습니다.",
        CONFLICT,
    ),
}

private const val BAD_REQUEST = 400
private const val PAYMENT_REQUIRED = 402
private const val CONFLICT = 409
