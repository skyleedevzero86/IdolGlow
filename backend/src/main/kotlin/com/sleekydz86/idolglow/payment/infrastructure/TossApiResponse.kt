package com.sleekydz86.idolglow.payment.infrastructure

import tools.jackson.databind.JsonNode

data class TossApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299

    val isInconclusive: Boolean
        get() =
            httpStatus == 0 ||
                httpStatus >= HTTP_SERVER_ERROR ||
                (
                    httpStatus == HTTP_CONFLICT &&
                        responseCode() == IDEMPOTENT_REQUEST_PROCESSING
                )

    private fun responseCode(): String? {
        val code = json?.get("code")?.takeUnless { it.isNull } ?: return null
        return code.asText().trim().ifBlank { null }
    }

    private companion object {
        const val HTTP_CONFLICT = 409
        const val HTTP_SERVER_ERROR = 500
        const val IDEMPOTENT_REQUEST_PROCESSING = "IDEMPOTENT_REQUEST_PROCESSING"
    }
}
