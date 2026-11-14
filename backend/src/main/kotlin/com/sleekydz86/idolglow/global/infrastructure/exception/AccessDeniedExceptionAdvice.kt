package com.sleekydz86.idolglow.global.infrastructure.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class AccessDeniedExceptionAdvice {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(request: HttpServletRequest): ResponseEntity<ExceptionResponse> {
        val authentication = SecurityContextHolder.getContext().authentication
        val anonymous =
            authentication == null ||
                !authentication.isAuthenticated ||
                authentication is AnonymousAuthenticationToken
        log.warn("접근 거부: {} {} | 로그인 여부={}", request.method, request.requestURI, !anonymous)

        return if (anonymous) {
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ExceptionResponse(name = "UNAUTHORIZED", errorCode = "UNAUTHORIZED", message = "로그인이 필요합니다."))
        } else {
            ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ExceptionResponse(name = "FORBIDDEN", errorCode = "FORBIDDEN", message = "이 기능을 사용할 권한이 없습니다."))
        }
    }
}
