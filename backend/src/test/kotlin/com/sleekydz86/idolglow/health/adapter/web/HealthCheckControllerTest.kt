package com.sleekydz86.idolglow.health.adapter.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HealthCheckControllerTest {
    @Test
    fun `헬스 JSON에 버전과 UP 상태를 반환한다`() {
        val controller = HealthCheckController(appVersion = "0.8.0")

        val body = controller.health()

        assertEquals("UP", body["status"])
        assertEquals("0.8.0", body["version"])
        assertEquals("idolglow-api", body["service"])
        assertEquals("정상", controller.healthCheck().body)
    }
}
