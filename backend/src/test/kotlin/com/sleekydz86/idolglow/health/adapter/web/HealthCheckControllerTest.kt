package com.sleekydz86.idolglow.health.adapter.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("헬스 체크")
class HealthCheckControllerTest {
    @DisplayName("헬스 JSON에 버전과 UP 상태를 반환한다")
    @Test
    fun returnsUpStatusAndVersion() {
        // given
        val controller = HealthCheckController(appVersion = "0.8.0")

        // when
        val body = controller.health()

        // then
        assertEquals("UP", body["status"])
        assertEquals("0.8.0", body["version"])
        assertEquals("idolglow-api", body["service"])
        assertEquals("정상", controller.healthCheck().body)
    }
}

