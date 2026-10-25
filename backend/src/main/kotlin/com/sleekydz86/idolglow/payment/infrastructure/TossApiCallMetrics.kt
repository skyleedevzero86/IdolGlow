package com.sleekydz86.idolglow.payment.infrastructure

import io.micrometer.core.instrument.MeterRegistry

internal fun tossApiCallOutcome(response: TossApiResponse): String =
    when {
        response.isSuccess2xx -> OUTCOME_SUCCESS
        response.isInconclusive -> OUTCOME_INCONCLUSIVE
        else -> OUTCOME_REJECTED
    }

internal fun recordTossApiCall(
    meterRegistry: MeterRegistry,
    api: String,
    response: TossApiResponse,
) {
    meterRegistry.counter(TOSS_API_CALLS, "api", api, "outcome", tossApiCallOutcome(response)).increment()
}

internal const val TOSS_API_CALLS = "toss.api.calls"

private const val OUTCOME_SUCCESS = "success"
private const val OUTCOME_REJECTED = "rejected"
private const val OUTCOME_INCONCLUSIVE = "inconclusive"
