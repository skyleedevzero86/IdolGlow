package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("Prometheus 지표 주소")
class PrometheusEndpointPostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("지표 주소는 로그인 없이 읽히고 공연 메타 버린 건수 지표가 들어 있다")
    @Test
    fun prometheusIsReadableWithoutLogin() {
        // given
        val path = "/actuator/prometheus"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("event_meta_projection_stale_dropped_total")))
    }

    @DisplayName("다른 actuator 주소는 열리지 않는다")
    @Test
    fun otherActuatorEndpointsStayClosed() {
        // given
        val path = "/actuator/env"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result.andExpect(status().isUnauthorized)
    }
}
