package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueArticleResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueDetailResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueFilterResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueSearchQuery
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicIssueSummaryResponse
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssueRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class IdolGlowPublicService(
    private val idolglowIssueRepository: IdolGlowIssueRepository,
) : IdolGlowPublicUseCase {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    override fun findIssues(
        query: PublicIssueSearchQuery,
        pageRequest: OffsetPageRequest,
    ): OffsetPageResult<PublicIssueSummaryResponse> {
        val matched = publishedIssues().filter(query::matches)
        val fromIndex = (pageRequest.page.toLong() * pageRequest.size).coerceAtMost(matched.size.toLong()).toInt()
        val toIndex = (fromIndex + pageRequest.size).coerceAtMost(matched.size)
        return OffsetPageResult.of(
            items = matched.subList(fromIndex, toIndex).map(PublicIssueSummaryResponse::from),
            request = pageRequest,
            totalElements = matched.size.toLong(),
        )
    }

    override fun findFilters(): PublicIssueFilterResponse = PublicIssueFilterResponse.from(publishedIssues())

    override fun findIssue(issueSlug: String): PublicIssueDetailResponse =
        PublicIssueDetailResponse.from(getPublishedIssue(issueSlug))

    override fun findArticle(
        issueSlug: String,
        articleSlug: String,
    ): PublicIssueArticleResponse {
        val issue = getPublishedIssue(issueSlug)
        val issueArticles = issue.articlesByLatest()
        val article =
            issueArticles.firstOrNull { it.slug == articleSlug }
                ?: throw EntityNotFoundException("공개된 웹진 기사를 찾을 수 없습니다. issueSlug=$issueSlug, articleSlug=$articleSlug")
        return PublicIssueArticleResponse.from(article, issueArticles)
    }

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun publishedIssues(): List<IdolGlowIssue> = idolglowIssueRepository.findAllPublishedWithArticles(today())

    private fun getPublishedIssue(issueSlug: String): IdolGlowIssue =
        idolglowIssueRepository
            .findBySlugWithArticles(issueSlug)
            ?.takeIf { it.isPublishedOn(today()) }
            ?: throw EntityNotFoundException("공개된 웹진 호를 찾을 수 없습니다. issueSlug=$issueSlug")
}
