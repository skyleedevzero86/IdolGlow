package com.sleekydz86.idolglow.glowcommunity.application.port.`in`

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer

interface GlowCommunityPostQueryUseCase {
    fun findPosts(
        query: GlowCommunityPostSearchQuery,
        viewer: GlowCommunityViewer,
    ): OffsetPageResult<GlowCommunityPostResult>

    fun findPost(
        postId: Long,
        viewer: GlowCommunityViewer,
    ): GlowCommunityPostResult

    fun findHighlights(viewer: GlowCommunityViewer): List<GlowCommunityPostResult>

    fun findMyPosts(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<GlowCommunityPostResult>
}
