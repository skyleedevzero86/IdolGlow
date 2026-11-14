package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue

data class PublicIssueSearchQuery(
    val year: Int? = null,
    val month: Int? = null,
    val volume: Int? = null,
    val keyword: String? = null,
) {
    private val normalizedKeyword: String? =
        keyword
            ?.trim()
            ?.take(MAX_KEYWORD_LENGTH)
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

    fun matches(issue: IdolGlowIssue): Boolean =
        (year == null || issue.issueDate.year == year) &&
            (month == null || issue.issueDate.monthValue == month) &&
            (volume == null || issue.volume == volume) &&
            matchesKeyword(issue)

    private fun matchesKeyword(issue: IdolGlowIssue): Boolean {
        val target = normalizedKeyword ?: return true
        val texts =
            sequenceOf(issue.teaser, "vol.${issue.volume}") +
                issue.articles.asSequence().flatMap { sequenceOf(it.title, it.kicker, it.summary) }
        return texts.any { it.lowercase().contains(target) }
    }

    companion object {
        private const val MAX_KEYWORD_LENGTH = 100
    }
}
