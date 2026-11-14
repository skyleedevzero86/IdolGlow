package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import com.sleekydz86.idolglow.like.application.dto.ContentLikeResult
import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import com.sleekydz86.idolglow.like.application.port.out.DelegatedLikePort
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.stereotype.Component

@Component
class GlowCommunityLikeDelegate(
    private val glowCommunityPostPort: GlowCommunityPostPort,
    private val glowCommunityReactionPort: GlowCommunityReactionPort,
    private val glowCommunityPostCommandUseCase: GlowCommunityPostCommandUseCase,
) : DelegatedLikePort {
    override val targetType: LikeTargetType = LikeTargetType.GLOW_COMMUNITY_POST

    override fun like(
        targetKey: String,
        userId: Long,
    ): ContentLikeResult = setLiked(postId(targetKey), userId, wanted = true)

    override fun unlike(
        targetKey: String,
        userId: Long,
    ): ContentLikeResult = setLiked(postId(targetKey), userId, wanted = false)

    override fun summarize(
        targetKeys: Collection<String>,
        viewerId: Long?,
    ): Map<String, LikeSummary> {
        val postIds = targetKeys.mapNotNull { it.toLongOrNull() }.distinct()
        if (postIds.isEmpty()) return emptyMap()
        val posts = glowCommunityPostPort.findAllByIds(postIds)
        val liked = viewerId?.let { glowCommunityReactionPort.findLikedPostIds(it, postIds) }.orEmpty()
        return posts.associate { post ->
            post.id.toString() to LikeSummary(likeCount = post.likeCount, liked = post.id in liked)
        }
    }

    private fun setLiked(
        postId: Long,
        userId: Long,
        wanted: Boolean,
    ): ContentLikeResult {
        val post =
            glowCommunityPostPort
                .findActiveById(postId)
                ?.takeIf { !it.blinded || it.isOwnedBy(userId) }
                ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
        val current = glowCommunityReactionPort.findLike(postId, userId) != null
        if (current == wanted) return ContentLikeResult(liked = current, likeCount = post.likeCount)
        val toggled = glowCommunityPostCommandUseCase.toggleLike(postId, userId)
        return ContentLikeResult(liked = toggled.liked, likeCount = toggled.likeCount)
    }

    private fun postId(targetKey: String): Long =
        targetKey.toLongOrNull() ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
}
