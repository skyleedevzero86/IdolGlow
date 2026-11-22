package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("관리자 수집 시각 새 칸 채우기 API")
class AdminEventCollectLogApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("관리자만 실행할 수 있고, 한 번에 옮길 건수가 잘못되면 400을 돌려준다")
    @Test
    fun onlyAdminCanRunBackfill() {
        // given
        val path = "/api/admin/event-meta/collect-log/time-zone-backfill"

        // when
        val anonymous = mockMvc.perform(post(path))
        val member = mockMvc.perform(post(path).with(user("2").roles("USER")))
        val admin = mockMvc.perform(post(path).param("batchSize", "500").with(user("1").roles("ADMIN")))
        val invalid = mockMvc.perform(post(path).param("batchSize", "0").with(user("1").roles("ADMIN")))

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        admin
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.remaining").value(0))
        invalid.andExpect(status().isBadRequest)
    }
}
