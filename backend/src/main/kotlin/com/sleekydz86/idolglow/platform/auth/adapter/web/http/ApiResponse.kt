package com.sleekydz86.idolglow.platform.auth.http

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    var success: Boolean = false,
    var message: String? = null,
    var data: T? = null,
    var errors: List<String>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var timestamp: LocalDateTime = LocalDateTime.now(),
    var requestId: String? = null,
    var statusCode: Int = 0,
) {
    companion object {
        @JvmStatic
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
                message = "성공",
                statusCode = 200,
                timestamp = LocalDateTime.now(),
            )

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> error(errorResponse: ErrorResponse): ApiResponse<T> =
            ApiResponse(
                success = false,
                data = errorResponse as T,
                message = errorResponse.message,
                statusCode = errorResponse.status,
                timestamp = LocalDateTime.now(),
            )
    }
}
