package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueVolumeResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.CreateIdolGlowIssueCommand
import com.sleekydz86.idolglow.idolglowissue.application.dto.UpsertIdolGlowArticleCommand

interface IdolGlowAdminUseCase {
    fun findIssues(
        page: Int,
        size: Int,
        year: Int?,
        month: Int?,
        volume: Int?,
    ): AdminIssuePageResponse

    fun findIssue(issueSlug: String): AdminIssueVolumeResponse

    fun findArticle(
        issueSlug: String,
        articleSlug: String,
    ): AdminIssueArticleResponse

    fun createIssue(command: CreateIdolGlowIssueCommand): AdminIssueVolumeResponse

    fun updateIssue(
        issueSlug: String,
        command: CreateIdolGlowIssueCommand,
    ): AdminIssueVolumeResponse

    fun deleteIssue(issueSlug: String)

    fun createArticle(
        issueSlug: String,
        command: UpsertIdolGlowArticleCommand,
    ): AdminIssueArticleResponse

    fun updateArticle(
        issueSlug: String,
        articleSlug: String,
        command: UpsertIdolGlowArticleCommand,
    ): AdminIssueArticleResponse

    fun deleteArticle(
        issueSlug: String,
        articleSlug: String,
    )
}
