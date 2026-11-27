package com.sleekydz86.idolglow.user.auth.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@DisplayName("비밀번호 로그인 API 권한")
class PasswordLoginSecurityPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

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

    @DisplayName("아이디 찾기는 토큰 없이 요청해도 200으로 응답한다")
    @Test
    fun findIdIsReachableWithoutToken() {
        // given
        val request = findIdRequest("nobody@example.com")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(OK, status)
    }

    @DisplayName("아이디 찾기는 가입한 이메일과 없는 이메일에 같은 응답을 준다")
    @Test
    fun findIdDoesNotRevealWhetherEmailIsRegistered() {
        // given
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val registered = userRepository.save(User(email = "f$token@test.com", nickname = Nickname.of("f$token")))

        // when
        val registeredBody = mockMvc.perform(findIdRequest(registered.email)).andReturn().response
        val unknownBody = mockMvc.perform(findIdRequest("none$token@test.com")).andReturn().response

        // then
        assertEquals(OK, registeredBody.status)
        assertEquals(OK, unknownBody.status)
        assertEquals(registeredBody.contentAsString, unknownBody.contentAsString)
    }

    @DisplayName("아이디 찾기도 GET 으로는 토큰 없이 열리지 않는다")
    @Test
    fun findIdGetIsNotPublic() {
        // given
        val request = get("/auth/account/find-id")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(UNAUTHORIZED, status)
    }

    private fun findIdRequest(email: String) =
        post("/auth/account/find-id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"$email"}""")

    companion object {
        private const val OK = 200
        private const val BAD_REQUEST = 400
        private const val UNAUTHORIZED = 401
    }
}
