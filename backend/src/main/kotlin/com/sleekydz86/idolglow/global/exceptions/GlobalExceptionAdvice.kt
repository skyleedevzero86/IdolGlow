package com.sleekydz86.idolglow.global.exceptions

import tools.jackson.databind.JsonMappingException
import io.minio.errors.ErrorResponseException
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionAdvice {

    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        request: HttpServletRequest,
        exception: MethodArgumentNotValidException
    ): ResponseEntity<ExceptionResponse> {
        log.warn("검증 실패: {} {}", request.method, request.requestURI)

        val errors = exception.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage ?: "유효하지 않음"}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ExceptionResponse(
                    name = "VALIDATION_ERROR",
                    errorCode = "BAD_REQUEST",
                    message = if (errors.isNotBlank()) errors else "요청 파라미터가 유효하지 않습니다."
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleDeserializationException(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ExceptionResponse> {
        val message = when (val cause = exception.cause) {
            is JsonMappingException -> {
                when {
                    cause.message?.contains("Required request body is missing") == true ->
                        "요청 본문이 필요합니다."

                    cause.message?.contains("null") == true -> {
                        val fieldPath = cause.path.joinToString(".") { it.fieldName }
                        "필수 필드 '$fieldPath'는 null일 수 없습니다."
                    }

                    else -> {
                        val fieldPath = cause.path.joinToString(".") { it.fieldName }
                        "JSON 형식이 올바르지 않습니다. (필드: $fieldPath)"
                    }
                }
            }

            else -> "요청 본문을 읽을 수 없습니다."
        }

        log.warn("역직렬화 오류: {}", message, exception)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ExceptionResponse(
                    name = "DESERIALIZATION_ERROR",
                    errorCode = "BAD_REQUEST",
                    message = message
                )
            )
    }

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(
        request: HttpServletRequest,
        exception: CustomException
    ): ResponseEntity<ExceptionResponse> {
        val type = exception.getExceptionType()

        log.warn(
            "커스텀 예외 발생: {} | URI: {} {} | 메시지: {}",
            type.errorCode, request.method, request.requestURI, type.message, exception
        )

        return ResponseEntity
            .status(type.httpStatusCode)
            .body(
                ExceptionResponse(
                    name = type::class.simpleName ?: type.errorCode,
                    errorCode = type.errorCode,
                    message = type.message
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        request: HttpServletRequest,
        exception: IllegalArgumentException
    ): ResponseEntity<ExceptionResponse> {
        log.warn("잘못된 요청: {} {} | {}", request.method, request.requestURI, exception.message)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ExceptionResponse(
                    name = "BAD_REQUEST",
                    errorCode = "BAD_REQUEST",
                    message = exception.message ?: "잘못된 요청입니다."
                )
            )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        request: HttpServletRequest,
        exception: IllegalStateException
    ): ResponseEntity<ExceptionResponse> {
        log.warn("충돌 발생: {} {} | {}", request.method, request.requestURI, exception.message)

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ExceptionResponse(
                    name = "CONFLICT",
                    errorCode = "CONFLICT",
                    message = exception.message ?: "요청을 처리할 수 없습니다."
                )
            )
    }

    @ExceptionHandler(ErrorResponseException::class)
    fun handleMinioErrorResponse(
        request: HttpServletRequest,
        exception: ErrorResponseException
    ): ResponseEntity<ExceptionResponse> {
        log.warn(
            "MinIO 오류: {} {} | code={} message={}",
            request.method,
            request.requestURI,
            exception.errorResponse().code(),
            exception.errorResponse().message(),
            exception
        )
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ExceptionResponse(
                    name = "PROFILE_IMAGE_STORAGE_UNAVAILABLE",
                    errorCode = "PROFILE_IMAGE_STORAGE_UNAVAILABLE",
                    message = UserExceptionType.PROFILE_IMAGE_STORAGE_UNAVAILABLE.message
                )
            )
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(
        request: HttpServletRequest,
        exception: EntityNotFoundException
    ): ResponseEntity<ExceptionResponse> {
        log.warn("대상을 찾을 수 없음: {} {} | {}", request.method, request.requestURI, exception.message)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ExceptionResponse(
                    name = "NOT_FOUND",
                    errorCode = "NOT_FOUND",
                    message = exception.message ?: "대상을 찾을 수 없습니다."
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        request: HttpServletRequest,
        exception: Exception
    ): ResponseEntity<ExceptionResponse> {
        log.error(
            "예상하지 못한 오류 발생: {} {} | 메시지: {}",
            request.method, request.requestURI, exception.message, exception
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ExceptionResponse(
                    name = "INTERNAL_SERVER_ERROR",
                    errorCode = "INTERNAL_SERVER_ERROR",
                    message = exception.message ?: "서버 내부 오류가 발생했습니다."
                )
            )
    }
}
