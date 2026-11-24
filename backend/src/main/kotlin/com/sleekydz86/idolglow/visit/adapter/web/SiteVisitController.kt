package com.sleekydz86.idolglow.visit.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.visit.application.SiteVisitService
import com.sleekydz86.idolglow.visit.domain.VisitorId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "사이트 방문", description = "로그인 여부와 상관없이 하루 한 번 방문을 기록하는 API")
@RestController
class SiteVisitController(
    private val siteVisitService: SiteVisitService,
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
    @Value("\${app.auth.refresh-cookie.secure:false}")
    private val secureCookie: Boolean,
) {
    @Operation(
        summary = "방문 기록",
        description =
            "방문자 식별 쿠키 ig_vid 로 서울 날짜 하루에 한 번만 기록합니다. 쿠키가 없거나 형식이 틀리면 새로 발급합니다. " +
                "로그인 상태면 회원 번호를, 그날 첫 요청이면 접속 IP 를 함께 남깁니다. 기록에 실패해도 204로 응답합니다.",
    )
    @PostMapping("/visits")
    @Suppress("TooGenericExceptionCaught")
    fun record(
        @CookieValue(name = VisitorId.COOKIE_NAME, required = false) rawVisitorId: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        val visitorId = siteVisitService.resolveVisitorId(rawVisitorId)
        try {
            siteVisitService.record(
                visitorId = visitorId,
                userId = authenticatedPrincipalResolver.resolveOrNull()?.userId,
                ipAddress = request.remoteAddr,
            )
        } catch (failure: RuntimeException) {
            log.warn("방문 기록 저장 실패 visitor={}", visitorId.take(LOG_ID_LENGTH), failure)
        }
        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, visitorCookie(visitorId).toString())
            .build()
    }

    private fun visitorCookie(visitorId: String): ResponseCookie =
        ResponseCookie
            .from(VisitorId.COOKIE_NAME, visitorId)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")
            .path("/")
            .maxAge(COOKIE_MAX_AGE)
            .build()

    companion object {
        private val log = LoggerFactory.getLogger(SiteVisitController::class.java)
        private val COOKIE_MAX_AGE: Duration = Duration.ofDays(365)
        private const val LOG_ID_LENGTH: Int = 8
    }
}
