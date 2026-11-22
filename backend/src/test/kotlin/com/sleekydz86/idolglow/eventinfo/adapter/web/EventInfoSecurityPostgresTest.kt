package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("행사 정보 API 권한")
class EventInfoSecurityPostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("행사 목록과 검색 조건 코드는 로그인 없이 요청할 수 있다")
    @Test
    fun listAndCodesArePublic() {
        // given
        val publicPaths =
            listOf(
                "/mypage/event-info/festivals",
                "/api/event-info/ldong-codes",
                "/mypage/event-info/culture-calendar/areas",
            )

        // when
        val statuses = publicPaths.associateWith { path -> mockMvc.perform(get(path)).andReturn().response.status }

        // then
        statuses.forEach { (path, status) ->
            assertTrue(status != UNAUTHORIZED && status != FORBIDDEN, "$path 가 로그인을 요구함: $status")
        }
    }

    @DisplayName("행사 상세는 로그인하지 않으면 401 을 돌려준다")
    @Test
    fun detailRequiresLogin() {
        // given
        val detailPaths =
            listOf(
                "/mypage/event-info/kopis/performances/PF000001",
                "/mypage/event-info/festivals/1234/common",
                "/api/event-info/sjw-perform/1",
            )

        // when
        val statuses = detailPaths.map { path -> mockMvc.perform(get(path)).andReturn().response.status }

        // then
        statuses.forEach { status -> assertEquals(UNAUTHORIZED, status) }
    }

    companion object {
        private const val UNAUTHORIZED = 401
        private const val FORBIDDEN = 403
    }
}
