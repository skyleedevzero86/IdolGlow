package com.sleekydz86.idolglow.partner.application.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class PartnerFeedQuery(
    val updatedSince: String?,
    val cursor: String?,
    val size: Int,
    val lang: String?,
)

data class PartnerEventItem(
    val globalId: String,
    val status: String,
    val mergedInto: String?,
    val locale: String,
    val title: String,
    val synopsis: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val venue: String?,
    val updatedAt: OffsetDateTime?,
    val externalIds: List<PartnerExternalId>,
)

data class PartnerSession(
    val startsAt: OffsetDateTime,
    val status: String,
)

data class PartnerExternalId(
    val scheme: String,
    val id: String,
)

data class PartnerEventPage(
    val items: List<PartnerEventItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class PartnerIssued(
    val id: Long,
    val name: String,
    val apiKey: String,
    val locales: List<String>,
    val rateLimitPerMinute: Int,
)

data class PartnerSummary(
    val id: Long,
    val name: String,
    val locales: List<String>,
    val rateLimitPerMinute: Int,
    val enabled: Boolean,
)
