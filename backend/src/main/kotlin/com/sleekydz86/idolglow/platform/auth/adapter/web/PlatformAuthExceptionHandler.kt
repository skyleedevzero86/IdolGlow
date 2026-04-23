package com.sleekydz86.idolglow.platform.auth.adapter.web

import com.sleekydz86.idolglow.platform.auth.http.ApiResponse
import com.sleekydz86.idolglow.platform.auth.http.ErrorResponse
import com.sleekydz86.idolglow.platform.user.domain.exception.AuthenticationFailedException
import com.sleekydz86.idolglow.platform.user.domain.exception.BasePlatformException
import com.sleekydz86.idolglow.platform.user.domain.exception.InvalidTokenException
import com.sleekydz86.idolglow.platform.user.domain.exception.UserAlreadyExistsException
import com.sleekydz86.idolglow.platform.user.domain.exception.UserNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice(basePackages = ["com.sleekydz86.idolglow.platform.auth.ui"])
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformAuthExceptionHandler {

    @ExceptionHandler(BasePlatformException::class)
    fun handleBasePlatform(
        ex: BasePlatformException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val status = when (ex) {
            is AuthenticationFailedException -> HttpStatus.UNAUTHORIZED
            is InvalidTokenException -> HttpStatus.UNAUTHORIZED
            is UserNotFoundException -> HttpStatus.NOT_FOUND
            is UserAlreadyExistsException -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        val errorCode = when (ex) {
            is AuthenticationFailedException -> "AUTHENTICATION_FAILED"
            is InvalidTokenException -> "INVALID_TOKEN"
            is UserNotFoundException -> "USER_NOT_FOUND"
            is UserAlreadyExistsException -> "USER_ALREADY_EXISTS"
            else -> "PLATFORM_ERROR"
        }
        val body = errorResponse(request, status.value(), errorCode, ex.message ?: "")
        return ResponseEntity.status(status).body(ApiResponse.error(body))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val body = errorResponse(request, HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.message ?: "")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(body))
    }

    private fun errorResponse(request: HttpServletRequest, status: Int, errorCode: String, message: String) =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status)
            .errorCode(errorCode)
            .message(message)
            .path(request.requestURI)
            .build()
}
