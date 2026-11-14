package com.sleekydz86.idolglow.glowcommunity.application.port.`in`

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityReportedPostResult

interface GlowCommunityAdminUseCase {
    fun findReportedPosts(request: OffsetPageRequest): OffsetPageResult<GlowCommunityReportedPostResult>

    fun blind(
        postId: Long,
        reason: String?,
    ): GlowCommunityReportedPostResult

    fun unblind(postId: Long): GlowCommunityReportedPostResult
}
