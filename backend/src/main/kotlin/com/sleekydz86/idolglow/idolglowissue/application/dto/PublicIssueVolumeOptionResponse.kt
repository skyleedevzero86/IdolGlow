package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue

data class PublicIssueVolumeOptionResponse(
    val slug: String,
    val volume: Int,
    val year: Int,
    val month: Int,
) {
    companion object {
        fun from(issue: IdolGlowIssue): PublicIssueVolumeOptionResponse =
            PublicIssueVolumeOptionResponse(
                slug = issue.slug,
                volume = issue.volume,
                year = issue.issueDate.year,
                month = issue.issueDate.monthValue,
            )
    }
}
