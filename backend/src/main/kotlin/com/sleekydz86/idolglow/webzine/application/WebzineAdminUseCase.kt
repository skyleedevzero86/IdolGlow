package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueVolumeResponse
import com.sleekydz86.idolglow.webzine.application.dto.CreateWebzineIssueCommand
import com.sleekydz86.idolglow.webzine.application.dto.UpsertWebzineArticleCommand

interface WebzineAdminUseCase {
    fun findIssues(
        page: Int,
        size: Int,
        year: Int?,
        month: Int?,
        volume: Int?,
    ): AdminIssuePageResponse

    fun findIssue(issueSlug: String): AdminIssueVolumeResponse

    fun findArticle(issueSlug: String, articleSlug: String): AdminIssueArticleResponse

    fun createIssue(command: CreateWebzineIssueCommand): AdminIssueVolumeResponse

    fun updateIssue(issueSlug: String, command: CreateWebzineIssueCommand): AdminIssueVolumeResponse

    fun deleteIssue(issueSlug: String)

    fun createArticle(issueSlug: String, command: UpsertWebzineArticleCommand): AdminIssueArticleResponse

    fun updateArticle(
        issueSlug: String,
        articleSlug: String,
        command: UpsertWebzineArticleCommand,
    ): AdminIssueArticleResponse

    fun deleteArticle(issueSlug: String, articleSlug: String)
}
