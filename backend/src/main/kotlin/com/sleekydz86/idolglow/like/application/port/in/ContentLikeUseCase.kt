package com.sleekydz86.idolglow.like.application.port.`in`

import com.sleekydz86.idolglow.like.application.dto.ContentLikeResult
import com.sleekydz86.idolglow.like.domain.LikeTarget

interface ContentLikeUseCase {
    fun like(
        userId: Long,
        target: LikeTarget,
    ): ContentLikeResult

    fun unlike(
        userId: Long,
        target: LikeTarget,
    ): ContentLikeResult
}
