package com.sleekydz86.idolglow.payment.infrastructure

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class TossApiCallMetricsTest {
    @Test
    fun countsTossApiCallByOutcome() {
        val registry = SimpleMeterRegistry()
        recordTossApiCall(registry, "confirm", TossApiResponse(200, null, "{}"))
        recordTossApiCall(registry, "confirm", TossApiResponse(400, null, "{}"))
        recordTossApiCall(registry, "getPaymentByOrderId", TossApiResponse(500, null, "{}"))
        val processing =
            ObjectMapper().readTree("""{"code":"IDEMPOTENT_REQUEST_PROCESSING"}""")
        recordTossApiCall(registry, "cancel", TossApiResponse(409, processing, "{}"))

        assertEquals(1.0, count(registry, "confirm", "success"))
        assertEquals(1.0, count(registry, "confirm", "rejected"))
        assertEquals(1.0, count(registry, "getPaymentByOrderId", "inconclusive"))
        assertEquals(1.0, count(registry, "cancel", "inconclusive"))
    }

    private fun count(
        registry: SimpleMeterRegistry,
        api: String,
        outcome: String,
    ): Double =
        registry
            .get(TOSS_API_CALLS)
            .tag("api", api)
            .tag("outcome", outcome)
            .counter()
            .count()
}
