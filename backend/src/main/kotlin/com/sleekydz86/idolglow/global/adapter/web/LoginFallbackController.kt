package com.sleekydz86.idolglow.global.adapter.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.view.RedirectView

@Controller
class LoginFallbackController(
    @Value("\${app.oauth2.redirect-uri}") private val redirectUri: String,
) {

    @GetMapping("/login")
    fun login(request: HttpServletRequest): RedirectView {
        val base = redirectUri.trim()
        val oauthFailed = request.parameterMap.containsKey("error")
        if (!oauthFailed) {
            return RedirectView(base, false, true)
        }
        val sep = if (base.contains("?")) "&" else "?"
        return RedirectView("$base${sep}oauth_error=1", false, true)
    }
}
