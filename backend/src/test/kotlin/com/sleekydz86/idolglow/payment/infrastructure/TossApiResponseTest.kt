package com.sleekydz86.idolglow.payment.infrastructure

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("토스 API 응답")
class TossApiResponseTest {
    private val objectMapper = ObjectMapper()

    @DisplayName("연결 실패·5xx·멱등 처리 중만 결과를 알 수 없는 응답이다")
    @Test
    fun tossApiResponseInconclusiveCases() {
        // given
        val processing = """{"code":"IDEMPOTENT_REQUEST_PROCESSING"}"""
        val otherConflict = """{"code":"ALREADY_PROCESSED"}"""

        // when
        val unknown =
            listOf(
                TossApiResponse(0, null, null),
                TossApiResponse(500, null, "internal"),
                TossApiResponse(503, null, "unavailable"),
                TossApiResponse(409, objectMapper.readTree(processing), processing),
            )
        val known =
            listOf(
                TossApiResponse(200, null, "{}"),
                TossApiResponse(400, null, """{"code":"REJECT_CARD"}"""),
                TossApiResponse(409, objectMapper.readTree(otherConflict), otherConflict),
            )

        // then
        unknown.forEach { assertTrue(it.isInconclusive) }
        known.forEach { assertFalse(it.isInconclusive) }
    }
}
