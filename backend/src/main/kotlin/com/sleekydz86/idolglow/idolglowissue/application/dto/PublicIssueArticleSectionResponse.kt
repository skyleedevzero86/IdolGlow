package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSection

data class PublicIssueArticleSectionResponse(
    val order: Int,
    val heading: String?,
    val paragraphs: List<String>,
    val note: String?,
) {
    companion object {
        fun from(section: IdolGlowArticleSection): PublicIssueArticleSectionResponse =
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
