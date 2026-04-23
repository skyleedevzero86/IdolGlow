package com.sleekydz86.idolglow.user.auth.adapter.oauth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginFailureHandler(
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val base = redirectUri.trim()
        val sep = if (base.contains("?")) "&" else "?"
        response.sendRedirect("$base${sep}oauth_error=1")
    }
}
