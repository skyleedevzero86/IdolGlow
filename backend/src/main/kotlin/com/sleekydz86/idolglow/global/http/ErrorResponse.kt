package com.sleekydz86.idolglow.platform.auth.http

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    var requestId: String? = null,
    var errorCode: String? = null,
    var message: String? = null,
    var path: String? = null,
    var status: Int = 0,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var timestamp: LocalDateTime = LocalDateTime.now(),
    var context: Map<String, Any>? = null,
    var traceId: String? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): ErrorResponseBuilder = ErrorResponseBuilder()
    }

    class ErrorResponseBuilder {
        private val errorResponse = ErrorResponse()

        fun requestId(requestId: String?) = apply { errorResponse.requestId = requestId }
        fun errorCode(errorCode: String?) = apply { errorResponse.errorCode = errorCode }
        fun message(message: String?) = apply { errorResponse.message = message }
        fun path(path: String?) = apply { errorResponse.path = path }
        fun status(status: Int) = apply { errorResponse.status = status }
        fun timestamp(timestamp: LocalDateTime) = apply { errorResponse.timestamp = timestamp }

        fun build(): ErrorResponse = errorResponse
    }
}
