package com.sleekydz86.idolglow.glowfeed.application.port.`in`

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedItemResult
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedCategory
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedHighlightSort

interface GlowFeedQueryUseCase {
    fun findFeed(
        category: GlowFeedCategory?,
        request: OffsetPageRequest,
        viewerId: Long?,
    ): OffsetPageResult<GlowFeedItemResult>

    fun findHighlights(
        sort: GlowFeedHighlightSort,
        viewerId: Long?,
    ): List<GlowFeedItemResult>
}
