package com.sleekydz86.idolglow.glowalert.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@DisplayName("Glow 알림 접근")
class GlowAlertAccessPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("로그인하지 않으면 알림 목록과 읽지 않은 수를 401로 막는다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["/api/glow-alerts", "/glow-alerts", "/mypage/glow-alerts", "/api/glow-alerts/unread-count"])
    fun rejectsAnonymousRead(path: String) {
        // given
        val request = get(path)

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(UNAUTHORIZED, status)
    }

    @DisplayName("로그인하지 않으면 읽음 처리를 401로 막는다")
    @Test
    fun rejectsAnonymousMarkRead() {
        // given
        val request = post("/api/glow-alerts/1/read")

        // when
        val status = mockMvc.perform(request).andReturn().response.status

        // then
        assertEquals(UNAUTHORIZED, status)
    }

    @DisplayName("로그인한 회원은 자기 알림만 받는다")
    @Test
    fun listsOnlyOwnAlerts() {
        // given
        val owner = persistUser()
        val other = persistUser()
        val mine = persistNotification(owner, "내 알림")
        persistNotification(other, "남의 알림")

        // when
        val response =
            mockMvc
                .perform(get("/api/glow-alerts").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andReturn()
                .response

        // then
        assertEquals(OK, response.status)
        val items = objectMapper.readTree(response.contentAsString).get("items")
        assertEquals(1, items.size())
        assertEquals(mine.id, items.get(0).get("id").asLong())
    }

    @DisplayName("남의 알림은 읽음으로 바꾸지 못한다")
    @Test
    fun cannotMarkOthersAlertRead() {
        // given
        val owner = persistUser()
        val intruder = persistUser()
        val alert = persistNotification(owner, "주인 알림")

        // when
        val status =
            mockMvc
                .perform(post("/api/glow-alerts/${alert.id}/read").header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andReturn()
                .response.status

        // then
        assertEquals(OK, status)
        assertNull(readAtOf(alert.id))
    }

    @DisplayName("자기 알림은 읽음으로 바뀐다")
    @Test
    fun marksOwnAlertRead() {
        // given
        val owner = persistUser()
        val alert = persistNotification(owner, "내 알림")

        // when
        val status =
            mockMvc
                .perform(post("/api/glow-alerts/${alert.id}/read").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andReturn()
                .response.status

        // then
        assertEquals(OK, status)
        assertNotNull(readAtOf(alert.id))
    }

    private fun persistUser(): User {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        return userRepository.save(User(email = "g$token@test.com", nickname = Nickname.of("g$token")))
    }

    private fun persistNotification(
        user: User,
        title: String,
    ): Notification =
        notificationRepository.save(
            Notification(
                userId = user.id,
                type = NotificationType.RESERVATION_CONFIRMED,
                title = title,
                message = "$title 내용",
            ),
        )

    private fun readAtOf(notificationId: Long): Any? =
        jdbcTemplate.queryForObject("select read_at from notifications where id = ?", Any::class.java, notificationId)

    private fun bearer(user: User): String = "Bearer ${jwtProvider.generateToken(user.id, UserRole.USER).accessToken}"

    companion object {
        private const val OK = 200
        private const val UNAUTHORIZED = 401
    }
}
