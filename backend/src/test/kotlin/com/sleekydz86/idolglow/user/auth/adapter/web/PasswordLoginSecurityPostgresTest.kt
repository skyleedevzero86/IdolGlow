package com.sleekydz86.idolglow.user.auth.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("비밀번호 로그인 API 권한")
class PasswordLoginSecurityPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("비밀번호 로그인은 토큰 없이 요청하면 보안 설정이 아니라 입력값 검사가 응답한다")
    @Test
    fun passwordLoginIsReachableWithoutToken() {

        // given
        val request =
            post("/auth/password/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"","password":""}""")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(BAD_REQUEST, status)
    }

    @DisplayName("비밀번호 로그인은 없는 계정이면 서비스가 401로 응답한다")
    @Test
    fun passwordLoginRejectsUnknownAccount() {

        // given
        val request =
            post("/auth/password/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@example.com","password":"wrong-password"}""")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(UNAUTHORIZED, status)
    }

    @DisplayName("임시 비밀번호 발급은 이번 변경과 관계없이 로그인을 요구한다")
    @Test
    fun temporaryPasswordStillRequiresLogin() {

        // given
        val request =
            post("/auth/password/temporary")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@example.com"}""")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(UNAUTHORIZED, status)
    }

    companion object {
        private const val BAD_REQUEST = 400
        private const val UNAUTHORIZED = 401
    }
}
