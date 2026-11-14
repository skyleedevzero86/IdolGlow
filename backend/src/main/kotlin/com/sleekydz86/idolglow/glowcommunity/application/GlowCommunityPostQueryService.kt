package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostQueryUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GlowCommunityPostQueryService(
    private val glowCommunityPostPort: GlowCommunityPostPort,
    private val glowCommunityPostAssembler: GlowCommunityPostAssembler,
) : GlowCommunityPostQueryUseCase {
    override fun findPosts(
        query: GlowCommunityPostSearchQuery,
        viewer: GlowCommunityViewer,
    ): OffsetPageResult<GlowCommunityPostResult> {
        val request = OffsetPageRequest.of(query.page, query.size)
        val keyword =
            query.keyword
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(MAX_KEYWORD_LENGTH)
        val normalized = query.copy(page = request.page, size = request.size, keyword = keyword)
        val slice = glowCommunityPostPort.search(normalized, viewer)
        return OffsetPageResult.of(
            items = glowCommunityPostAssembler.toResults(slice.items, viewer),
            request = request,
            totalElements = slice.totalElements,
        )
    }

    override fun findPost(
        postId: Long,
        viewer: GlowCommunityViewer,
    ): GlowCommunityPostResult {
        val post =
            glowCommunityPostPort
                .findActiveById(postId)
                ?.takeIf { it.visibleTo(viewer.userId, viewer.admin) }
                ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
        return glowCommunityPostAssembler.toResult(post, viewer)
    }

    override fun findHighlights(viewer: GlowCommunityViewer): List<GlowCommunityPostResult> =
        glowCommunityPostAssembler.toResults(glowCommunityPostPort.findHighlights(HIGHLIGHT_LIMIT), viewer)

    override fun findMyPosts(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<GlowCommunityPostResult> {
        val normalized = OffsetPageRequest.of(request.page, request.size)
        val slice = glowCommunityPostPort.findByUserId(userId, normalized)
        return OffsetPageResult.of(
            items = glowCommunityPostAssembler.toResults(slice.items, GlowCommunityViewer.member(userId)),
            request = normalized,
            totalElements = slice.totalElements,
        )
    }

    companion object {
        const val HIGHLIGHT_LIMIT = 8
        private const val MAX_KEYWORD_LENGTH = 100
    }
}
