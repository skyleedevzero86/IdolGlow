package com.sleekydz86.idolglow.idolglowissue.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.idolglowissue.application.IdolGlowArticleRankingUseCase
import com.sleekydz86.idolglow.idolglowissue.application.dto.PublicRankedArticleResponse
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "웹진", description = "발행된 Idol Glow 웹진 호·기사 공개 조회 API")
@RestController
@RequestMapping("/api/idolglow/articles")
class IdolGlowArticleController(
    private val idolglowArticleRankingUseCase: IdolGlowArticleRankingUseCase,
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) {
    @Operation(
        summary = "발행된 웹진 기사 카드 목록",
        description =
            "로그인 없이 볼 수 있습니다. sort 는 latest(발행일 최신), popular(좋아요 수 + 본 회원 수), " +
                "likes(좋아요 수) 입니다.",
    )
    @GetMapping
    fun findArticles(
        @RequestParam(defaultValue = "latest") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_PAGE_SIZE") size: Int,
    ): OffsetPageResult<PublicRankedArticleResponse> =
        idolglowArticleRankingUseCase.findArticles(
            IdolGlowArticleSort.fromCode(sort),
            OffsetPageRequest.of(page, size),
            authenticatedPrincipalResolver.resolveOrNull()?.userId,
        )

    companion object {
        private const val DEFAULT_PAGE_SIZE = 12
    }
}
