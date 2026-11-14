package com.sleekydz86.idolglow.like.application.port.`in`

import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import com.sleekydz86.idolglow.like.domain.LikeTarget

interface ContentLikeQueryUseCase {
    fun summarize(
        targets: Collection<LikeTarget>,
        viewerId: Long?,
    ): Map<LikeTarget, LikeSummary>
}
