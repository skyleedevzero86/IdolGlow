package com.sleekydz86.idolglow.servermetric.application

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class HttpResponseCounter(
    private val meterRegistry: MeterRegistry,
) {
    private var lastTotal: Long? = null
    private var lastErrors: Long? = null

    @Synchronized
    fun takeSinceLast(): HttpResponseDelta {
        val total = cumulative(null)
        val errors = cumulative(SERVER_ERROR_OUTCOME)
        val delta = HttpResponseDelta(deltaOf(total, lastTotal), deltaOf(errors, lastErrors))
        lastTotal = total
        lastErrors = errors
        return delta
    }

    private fun cumulative(outcome: String?): Long {
        val search = meterRegistry.find(HTTP_SERVER_REQUESTS)
        val timers = if (outcome == null) search.timers() else search.tag(OUTCOME_TAG, outcome).timers()
        return timers.sumOf { it.count() }
    }

    companion object {
        const val HTTP_SERVER_REQUESTS: String = "http.server.requests"
        const val OUTCOME_TAG: String = "outcome"
        const val SERVER_ERROR_OUTCOME: String = "SERVER_ERROR"

        fun deltaOf(
            current: Long,
            previous: Long?,
        ): Long = if (previous == null || current < previous) current else current - previous
    }
}

data class HttpResponseDelta(
    val requests: Long,
    val errors: Long,
)
