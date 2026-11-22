package com.sleekydz86.idolglow.glowcommunity.application.port.`in`

import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityLikeResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.UpdateGlowCommunityPostCommand

interface GlowCommunityPostCommandUseCase {
    fun create(command: CreateGlowCommunityPostCommand): GlowCommunityPostResult

    fun update(command: UpdateGlowCommunityPostCommand): GlowCommunityPostResult

    fun delete(
        postId: Long,
        userId: Long,
    )

    fun toggleLike(
        postId: Long,
        userId: Long,
    ): GlowCommunityLikeResult

    fun report(
        postId: Long,
        userId: Long,
        reason: String?,
    )
}
