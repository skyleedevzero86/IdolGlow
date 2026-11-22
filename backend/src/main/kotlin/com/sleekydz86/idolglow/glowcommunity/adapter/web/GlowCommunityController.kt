package com.sleekydz86.idolglow.glowcommunity.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedPrincipalResolver
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.adapter.web.request.GlowCommunityPostRequest
import com.sleekydz86.idolglow.glowcommunity.adapter.web.request.GlowCommunityReportRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordsResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityLikeResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.dto.UpdateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityKeywordUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostQueryUseCase
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunitySort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "GLOW 지역사회", description = "지역사회 게시판 글·좋아요·신고·추천 검색어 API")
@RestController
@RequestMapping("/api/glow-community")
class GlowCommunityController(
    private val glowCommunityPostQueryUseCase: GlowCommunityPostQueryUseCase,
    private val glowCommunityPostCommandUseCase: GlowCommunityPostCommandUseCase,
    private val glowCommunityKeywordUseCase: GlowCommunityKeywordUseCase,
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) {
    @Operation(summary = "게시글 목록", description = "로그인 없이 조회합니다. page 는 0부터 시작합니다.")
    @GetMapping("/posts")
    fun findPosts(
        @RequestParam(defaultValue = "all") tag: String,
        @RequestParam(defaultValue = "latest") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
    ): OffsetPageResult<GlowCommunityPostResult> =
        glowCommunityPostQueryUseCase.findPosts(
            GlowCommunityPostSearchQuery(
                tag = GlowCommunityTag.fromFilter(tag),
                sort = GlowCommunitySort.fromCode(sort),
                keyword = q,
                page = page,
                size = size,
            ),
            viewer(),
        )

    @Operation(summary = "게시글 상세", description = "블라인드 글은 작성자와 관리자만 볼 수 있습니다.")
    @GetMapping("/posts/{postId}")
    fun findPost(
        @PathVariable postId: Long,
    ): GlowCommunityPostResult = glowCommunityPostQueryUseCase.findPost(postId, viewer())

    @Operation(summary = "하이라이트", description = "좋아요 수 또는 댓글 수가 많은 글 최대 8개")
    @GetMapping("/highlights")
    fun findHighlights(): List<GlowCommunityPostResult> = glowCommunityPostQueryUseCase.findHighlights(viewer())

    @Operation(summary = "추천 검색어", description = "최근·인기 글을 바탕으로 만든 검색어. 주기적으로 갱신됩니다.")
    @GetMapping("/keywords")
    fun findKeywords(): GlowCommunityKeywordsResult = glowCommunityKeywordUseCase.findKeywords()

    @Operation(summary = "내 게시글", description = "블라인드된 내 글도 blinded 표시와 함께 보여 줍니다.")
    @GetMapping("/me/posts")
    fun findMyPosts(
        @LoginUser userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): OffsetPageResult<GlowCommunityPostResult> =
        glowCommunityPostQueryUseCase.findMyPosts(userId, OffsetPageRequest.of(page, size))

    @Operation(summary = "게시글 작성")
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @LoginUser userId: Long,
        @Valid @RequestBody request: GlowCommunityPostRequest,
    ): GlowCommunityPostResult =
        glowCommunityPostCommandUseCase.create(
            CreateGlowCommunityPostCommand(
                userId = userId,
                tag = GlowCommunityTag.fromCode(request.tag),
                title = request.title,
                content = request.content,
            ),
        )

    @Operation(summary = "게시글 수정", description = "본인 글만 수정할 수 있습니다.")
    @PutMapping("/posts/{postId}")
    fun update(
        @LoginUser userId: Long,
        @PathVariable postId: Long,
        @Valid @RequestBody request: GlowCommunityPostRequest,
    ): GlowCommunityPostResult =
        glowCommunityPostCommandUseCase.update(
            UpdateGlowCommunityPostCommand(
                postId = postId,
                userId = userId,
                tag = GlowCommunityTag.fromCode(request.tag),
                title = request.title,
                content = request.content,
            ),
        )

    @Operation(summary = "게시글 삭제", description = "본인 글만 삭제할 수 있습니다.")
    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @LoginUser userId: Long,
        @PathVariable postId: Long,
    ) {
        glowCommunityPostCommandUseCase.delete(postId, userId)
    }

    @Operation(summary = "좋아요 토글", description = "한 회원은 글마다 좋아요 하나만 남길 수 있습니다.")
    @PostMapping("/posts/{postId}/like")
    fun toggleLike(
        @LoginUser userId: Long,
        @PathVariable postId: Long,
    ): GlowCommunityLikeResult = glowCommunityPostCommandUseCase.toggleLike(postId, userId)

    @Operation(summary = "게시글 신고", description = "같은 회원의 중복 신고는 무시합니다. 서로 다른 회원 3명이 신고하면 자동 블라인드됩니다.")
    @PostMapping("/posts/{postId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun report(
        @LoginUser userId: Long,
        @PathVariable postId: Long,
        @Valid @RequestBody(required = false) request: GlowCommunityReportRequest?,
    ) {
        glowCommunityPostCommandUseCase.report(postId, userId, request?.reason)
    }

    private fun viewer(): GlowCommunityViewer {
        val principal = authenticatedPrincipalResolver.resolveOrNull() ?: return GlowCommunityViewer.ANONYMOUS
        return GlowCommunityViewer(userId = principal.userId, admin = principal.hasRole(ADMIN_ROLE))
    }

    companion object {
        private const val ADMIN_ROLE = "ADMIN"
    }
}
