package com.sleekydz86.idolglow.webzine.adapter.web

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.webzine.application.WebzinePublicUseCase
import com.sleekydz86.idolglow.webzine.application.dto.PublicIssueArticleResponse
import com.sleekydz86.idolglow.webzine.application.dto.PublicIssueDetailResponse
import com.sleekydz86.idolglow.webzine.application.dto.PublicIssueFilterResponse
import com.sleekydz86.idolglow.webzine.application.dto.PublicIssueSearchQuery
import com.sleekydz86.idolglow.webzine.application.dto.PublicIssueSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "웹진", description = "발행된 Idol Glow 웹진 호·기사 공개 조회 API")
@RestController
@RequestMapping("/api/webzine/issues")
class WebzineIssueController(
    private val webzinePublicUseCase: WebzinePublicUseCase,
) {
    @Operation(summary = "발행된 웹진 호 목록 조회")
    @GetMapping
    fun findIssues(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_PAGE_SIZE") size: Int,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
        @RequestParam(required = false) volume: Int?,
        @RequestParam(required = false) keyword: String?,
    ): OffsetPageResult<PublicIssueSummaryResponse> =
        webzinePublicUseCase.findIssues(
            PublicIssueSearchQuery(year = year, month = month, volume = volume, keyword = keyword),
            OffsetPageRequest.of(page, size),
        )

    @Operation(summary = "발행된 웹진 호 찾기 조건 조회")
    @GetMapping("/filters")
    fun findFilters(): PublicIssueFilterResponse = webzinePublicUseCase.findFilters()

    @Operation(summary = "발행된 웹진 호 상세 조회")
    @GetMapping("/{issueSlug}")
    fun findIssue(
        @PathVariable issueSlug: String,
    ): PublicIssueDetailResponse = webzinePublicUseCase.findIssue(issueSlug)

    @Operation(summary = "발행된 웹진 기사 상세 조회")
    @GetMapping("/{issueSlug}/articles/{articleSlug}")
    fun findArticle(
        @PathVariable issueSlug: String,
        @PathVariable articleSlug: String,
    ): PublicIssueArticleResponse = webzinePublicUseCase.findArticle(issueSlug, articleSlug)

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
    }
}
