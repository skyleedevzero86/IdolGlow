package com.sleekydz86.idolglow.global.exceptions.gemma

import com.sleekydz86.idolglow.global.exceptions.CustomExceptionType

enum class GemmaExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    GEMMA_DISABLED(
        "GEMMA_DISABLED",
        "Gemma 연동이 비활성화되어 있습니다.",
        503,
    ),

    GEMMA_PROVIDER_UNAVAILABLE(
        "GEMMA_PROVIDER_UNAVAILABLE",
        "Gemma 서버 호출에 실패했습니다.",
        503,
    ),

    GEMMA_EMPTY_RESPONSE(
        "GEMMA_EMPTY_RESPONSE",
        "Gemma 응답에서 텍스트를 추출하지 못했습니다.",
        502,
    ),
}
