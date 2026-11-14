package com.sleekydz86.idolglow.like.application.port.out

import com.sleekydz86.idolglow.like.application.dto.ContentLikeResult
import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import com.sleekydz86.idolglow.like.domain.LikeTargetType

interface DelegatedLikePort {
    val targetType: LikeTargetType

    fun like(
        targetKey: String,
        userId: Long,
    ): ContentLikeResult

    fun unlike(
        targetKey: String,
        userId: Long,
    ): ContentLikeResult

    fun summarize(
        targetKeys: Collection<String>,
        viewerId: Long?,
    ): Map<String, LikeSummary>
}
