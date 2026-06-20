package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection

data class AdminIssueArticleSectionResponse(
    val id: Long,
    val heading: String?,
    val body: String,
    val paragraphs: List<String>,
    val note: String?,
) {
    companion object {
        fun from(section: WebzineArticleSection): AdminIssueArticleSectionResponse =
            AdminIssueArticleSectionResponse(
                id = section.id,
                heading = section.heading,
                body = section.body,
                paragraphs =
                    section.body
                        .split(Regex("\\n{2,}"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                note = section.note,
            )
    }
}
