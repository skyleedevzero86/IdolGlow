package com.sleekydz86.idolglow.global.infrastructure.exception.subscription

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class SubscriptionAdminExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    DISPATCH_NOT_FOUND(
        "SUBSCRIPTION_DISPATCH_NOT_FOUND",
        "발송 이력을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    MAIL_LOG_NOT_FOUND(
        "MAIL_SEND_LOG_NOT_FOUND",
        "메일 발송 기록을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
}

private const val NOT_FOUND = 404
