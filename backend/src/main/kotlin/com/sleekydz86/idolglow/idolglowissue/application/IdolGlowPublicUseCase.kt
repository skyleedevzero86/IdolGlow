package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueArticleResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueDetailResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueFilterResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueSearchQuery
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueSummaryResponse

interface IdolGlowPublicUseCase {
    fun findIssues(
        query: PublicIssueSearchQuery,
        pageRequest: OffsetPageRequest,
    ): OffsetPageResult<PublicIssueSummaryResponse>

    fun findFilters(): PublicIssueFilterResponse

    fun findIssue(issueSlug: String): PublicIssueDetailResponse

    fun findArticle(
        issueSlug: String,
        articleSlug: String,
    ): PublicIssueArticleResponse
}
