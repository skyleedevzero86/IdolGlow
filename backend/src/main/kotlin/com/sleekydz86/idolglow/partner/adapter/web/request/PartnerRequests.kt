package com.sleekydz86.idolglow.partner.adapter.web.request

data class PartnerIssueRequest(
    val name: String = "",
    val locales: List<String> = emptyList(),
    val rateLimitPerMinute: Int = DEFAULT_RATE_LIMIT,
)

data class PartnerEnabledRequest(
    val enabled: Boolean = true,
)

private const val DEFAULT_RATE_LIMIT = 60
