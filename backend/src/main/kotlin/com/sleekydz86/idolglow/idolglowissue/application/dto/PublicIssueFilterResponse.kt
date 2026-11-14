package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue

data class PublicIssueFilterResponse(
    val latestVolume: Int?,
    val latestIssueSlug: String?,
    val years: List<Int>,
    val months: Map<Int, List<Int>>,
    val volumes: List<PublicIssueVolumeOptionResponse>,
) {
    companion object {
        fun from(publishedIssues: List<IdolGlowIssue>): PublicIssueFilterResponse {
            val latest = publishedIssues.maxByOrNull { it.volume }
            val years = publishedIssues.map { it.issueDate.year }.distinct().sortedDescending()
            return PublicIssueFilterResponse(
                latestVolume = latest?.volume,
                latestIssueSlug = latest?.slug,
                years = years,
                months =
                    years.associateWith { year ->
                        publishedIssues
                            .filter { it.issueDate.year == year }
                            .map { it.issueDate.monthValue }
                            .distinct()
                            .sorted()
                    },
                volumes =
                    publishedIssues
                        .sortedByDescending { it.volume }
                        .map(PublicIssueVolumeOptionResponse::from),
            )
        }
    }
}
