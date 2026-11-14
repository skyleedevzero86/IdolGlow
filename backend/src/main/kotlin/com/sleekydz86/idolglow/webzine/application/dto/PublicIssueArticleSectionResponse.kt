package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection

data class PublicIssueArticleSectionResponse(
    val order: Int,
    val heading: String?,
    val paragraphs: List<String>,
    val note: String?,
) {
    companion object {
        fun from(section: WebzineArticleSection): PublicIssueArticleSectionResponse =
            PublicIssueArticleSectionResponse(
                order = section.displayOrder,
                heading = section.heading,
                paragraphs =
                    section.body
                        .split(Regex("\\n{2,}"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                note = section.note,
            )
    }
}
