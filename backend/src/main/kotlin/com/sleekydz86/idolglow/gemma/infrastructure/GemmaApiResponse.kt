package com.sleekydz86.idolglow.gemma.infrastructure

import tools.jackson.databind.JsonNode

data class GemmaApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299
}
