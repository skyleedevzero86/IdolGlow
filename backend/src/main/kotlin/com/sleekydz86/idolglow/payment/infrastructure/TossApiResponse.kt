package com.sleekydz86.idolglow.payment.infrastructure

import tools.jackson.databind.JsonNode

data class TossApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299
}
