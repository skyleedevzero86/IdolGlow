package com.sleekydz86.idolglow.global.infrastructure.exception.partner

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class PartnerExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    KEY_MISSING("PARTNER_KEY_MISSING", "파트너 키가 필요합니다. X-Partner-Key 헤더에 넣어 주세요.", UNAUTHORIZED),
    KEY_INVALID("PARTNER_KEY_INVALID", "파트너 키가 올바르지 않습니다.", UNAUTHORIZED),
    DISABLED("PARTNER_DISABLED", "사용이 중지된 파트너 키입니다.", FORBIDDEN),
    RATE_LIMITED("PARTNER_RATE_LIMITED", "분당 호출 한도를 넘었습니다. 잠시 뒤 다시 호출해 주세요.", TOO_MANY_REQUESTS),
    LOCALE_NOT_CONTRACTED("PARTNER_LOCALE_NOT_CONTRACTED", "계약하지 않은 언어입니다.", BAD_REQUEST),
    EVENT_NOT_FOUND("PARTNER_EVENT_NOT_FOUND", "공연을 찾을 수 없습니다.", NOT_FOUND),
    PARTNER_NOT_FOUND("PARTNER_NOT_FOUND", "파트너를 찾을 수 없습니다.", NOT_FOUND),
}

class PartnerRateLimitedException(
    val retryAfterSeconds: Long,
) : CustomException(PartnerExceptionType.RATE_LIMITED)

private const val BAD_REQUEST = 400
private const val UNAUTHORIZED = 401
private const val FORBIDDEN = 403
private const val NOT_FOUND = 404
private const val TOO_MANY_REQUESTS = 429
