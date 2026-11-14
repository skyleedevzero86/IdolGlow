package com.sleekydz86.idolglow.glowfeed.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedItemResult
import com.sleekydz86.idolglow.glowfeed.application.port.`in`.GlowFeedQueryUseCase
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedCategory
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedHighlightSort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "GLOW 전체 보기", description = "트렌드·행사·리뷰·지역사회 글을 한 번에 보는 API")
@RestController
@RequestMapping("/api/glow/feed")
class GlowFeedController(
    private val glowFeedQueryUseCase: GlowFeedQueryUseCase,
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) {
    @Operation(
        summary = "전체 카드 목록",
        description =
            "로그인 없이 볼 수 있고 최신순입니다. category 는 all, trend, news, recent, local 입니다. " +
                "전체 보기에서 행사는 최근 등록된 6건만 섞습니다.",
    )
    @GetMapping
    fun findFeed(
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
    ): OffsetPageResult<GlowFeedItemResult> =
        glowFeedQueryUseCase.findFeed(
            GlowFeedCategory.fromFilter(category),
            OffsetPageRequest.of(page, size),
            authenticatedPrincipalResolver.resolveOrNull()?.userId,
        )

    @Operation(
        summary = "카테고리별 대표 카드",
        description = "latest 는 카테고리마다 가장 최근 글, popular 는 좋아요 수와 본 회원 수의 합이 가장 큰 글입니다.",
    )
    @GetMapping("/highlights")
    fun findHighlights(
        @RequestParam(defaultValue = "latest") sort: String,
    ): List<GlowFeedItemResult> =
        glowFeedQueryUseCase.findHighlights(
            GlowFeedHighlightSort.fromCode(sort),
            authenticatedPrincipalResolver.resolveOrNull()?.userId,
        )
}
