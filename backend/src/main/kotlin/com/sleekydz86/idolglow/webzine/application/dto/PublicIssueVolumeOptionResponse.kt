package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue

data class PublicIssueVolumeOptionResponse(
    val slug: String,
    val volume: Int,
    val year: Int,
    val month: Int,
) {
    companion object {
        fun from(issue: WebzineIssue): PublicIssueVolumeOptionResponse =
            PublicIssueVolumeOptionResponse(
                slug = issue.slug,
                volume = issue.volume,
                year = issue.issueDate.year,
                month = issue.issueDate.monthValue,
            )
    }
}
