package com.sleekydz86.idolglow.global.adapter.security

import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.exception.ExceptionResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(
                ExceptionResponse(
                    name = "UNAUTHENTICATED",
                    errorCode = "UNAUTHENTICATED",
                    message = "로그인이 필요합니다."
                )
            )
        )
    }
}
