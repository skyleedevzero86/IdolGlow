package com.sleekydz86.idolglow.admin.adapter.web

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("관리자 서버 상태 응답 모양 고정")
class AdminServerStatusContractPostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("인프라 네 가지의 종류·이름·상태와 전체 상태, 요약·시스템·액추에이터 칸이 그대로 나온다")
    @Test
    fun keepsServerStatusShape() {
        // given
        val request = get("/admin/server-status").with(user("1").roles("ADMIN"))

        // when
        val result = mockMvc.perform(request)

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.generatedAt").isString)
            .andExpect(jsonPath("$.overallStatus").value("UP"))
            .andExpect(jsonPath("$.infrastructure.length()").value(4))
            .andExpect(jsonPath("$.infrastructure[0].type").value("DATABASE"))
            .andExpect(jsonPath("$.infrastructure[0].label").value("데이터베이스"))
            .andExpect(jsonPath("$.infrastructure[0].status").value("UP"))
            .andExpect(jsonPath("$.infrastructure[0].message").value("데이터베이스 연결이 정상입니다."))
            .andExpect(jsonPath("$.infrastructure[0].responseTimeMs").isNumber)
            .andExpect(jsonPath("$.infrastructure[0].details.product").value("PostgreSQL"))
            .andExpect(jsonPath("$.infrastructure[1].type").value("MINIO"))
            .andExpect(jsonPath("$.infrastructure[1].status").value("NOT_CONFIGURED"))
            .andExpect(jsonPath("$.infrastructure[2].type").value("REDIS"))
            .andExpect(jsonPath("$.infrastructure[2].status").value("NOT_CONFIGURED"))
            .andExpect(jsonPath("$.infrastructure[3].type").value("MQ"))
            .andExpect(jsonPath("$.infrastructure[3].label").value("메시지 큐"))
            .andExpect(jsonPath("$.infrastructure[3].status").value("NOT_CONFIGURED"))
            .andExpect(jsonPath("$.summary.uptimeSeconds").isNumber)
            .andExpect(jsonPath("$.system.jvm.liveThreadCount").isNumber)
            .andExpect(jsonPath("$.actuator.metrics.length()").value(6))
    }
}
