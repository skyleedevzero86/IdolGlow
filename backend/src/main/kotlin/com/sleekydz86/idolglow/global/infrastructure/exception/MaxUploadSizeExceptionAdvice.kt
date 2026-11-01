package com.sleekydz86.idolglow.global.infrastructure.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class MaxUploadSizeExceptionAdvice {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        request: HttpServletRequest,
        exception: MaxUploadSizeExceededException,
    ): ResponseEntity<ExceptionResponse> {
        log.warn(
            "업로드 용량 초과: {} {} | maxBytes={}",
            request.method,
            request.requestURI,
            exception.maxUploadSize,
        )
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(
                ExceptionResponse(
                    name = "PAYLOAD_TOO_LARGE",
                    errorCode = "PAYLOAD_TOO_LARGE",
                    message = "이미지 업로드는 최대 150MB까지 가능합니다.",
                ),
            )
    }
}
