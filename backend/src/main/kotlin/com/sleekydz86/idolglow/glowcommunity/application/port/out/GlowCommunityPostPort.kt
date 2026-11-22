package com.sleekydz86.idolglow.glowcommunity.application.port.out

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost

interface GlowCommunityPostPort {
    fun save(post: GlowCommunityPost): GlowCommunityPost

    fun findActiveById(postId: Long): GlowCommunityPost?

    fun findActiveByIdForUpdate(postId: Long): GlowCommunityPost?

    fun findAllByIds(postIds: Collection<Long>): List<GlowCommunityPost>

    fun search(
        query: GlowCommunityPostSearchQuery,
        viewer: GlowCommunityViewer,
    ): OffsetSlice<GlowCommunityPost>

    fun findHighlights(limit: Int): List<GlowCommunityPost>

    fun findByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetSlice<GlowCommunityPost>

    fun findReported(request: OffsetPageRequest): OffsetSlice<GlowCommunityPost>

    fun findRecentVisible(limit: Int): List<GlowCommunityPost>

    fun findPopularVisible(limit: Int): List<GlowCommunityPost>
}
