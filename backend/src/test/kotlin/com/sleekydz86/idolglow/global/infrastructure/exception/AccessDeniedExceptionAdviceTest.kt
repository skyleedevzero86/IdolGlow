package com.sleekydz86.idolglow.global.infrastructure.exception

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("권한 거부 응답")
class AccessDeniedExceptionAdviceTest {
    private val advice = AccessDeniedExceptionAdvice()
    private val request = MockHttpServletRequest("GET", "/admin/users")

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
    }

    @DisplayName("로그인한 일반 회원이 관리자 기능에 접근하면 403 을 돌려준다")
    @Test
    fun loggedInMemberGetsForbidden() {
        // given
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("1", null, listOf(SimpleGrantedAuthority("ROLE_USER")))

        // when
        val response = advice.handleAccessDeniedException(request)

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("FORBIDDEN", response.body?.errorCode)
    }

    @DisplayName("로그인하지 않은 사용자가 접근하면 401 을 돌려준다")
    @Test
    fun anonymousGetsUnauthorized() {
        // given
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        // when
        val response = advice.handleAccessDeniedException(request)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("UNAUTHORIZED", response.body?.errorCode)
    }

    @DisplayName("인증 정보가 없으면 401 을 돌려준다")
    @Test
    fun missingAuthenticationGetsUnauthorized() {
        // given
        SecurityContextHolder.clearContext()

        // when
        val response = advice.handleAccessDeniedException(request)

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
