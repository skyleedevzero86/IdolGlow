package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.comment.application.port.out.CommentTargetPolicy
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import org.springframework.stereotype.Component

@Component
class GlowCommunityCommentTargetPolicy(
    private val glowCommunityPostPort: GlowCommunityPostPort,
) : CommentTargetPolicy {
    override val targetType: ContentTargetType = ContentTargetType.GLOW_COMMUNITY_POST

    override fun checkReadable(
        targetKey: String,
        viewerId: Long?,
        admin: Boolean,
    ) {
        postIdOf(targetKey)
            ?.let(glowCommunityPostPort::findActiveById)
            ?.takeIf { it.visibleTo(viewerId, admin) }
            ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
    }

    override fun checkWritable(
        targetKey: String,
        userId: Long,
    ) {
        val post =
            lockedPost(targetKey)
                ?.takeIf { it.visibleTo(userId, admin = false) }
                ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
        if (post.blinded) throw CustomException(GlowCommunityExceptionType.BLINDED_POST)
    }

    override fun commentAdded(targetKey: String) {
        lockedPost(targetKey)?.let { post ->
            post.adjustCommentCount(1L)
            glowCommunityPostPort.save(post)
        }
    }

    override fun commentRemoved(targetKey: String) {
        lockedPost(targetKey)?.let { post ->
            post.adjustCommentCount(-1L)
            glowCommunityPostPort.save(post)
        }
    }

    override fun ownerOf(targetKey: String): Long? = activePost(targetKey)?.userId

    override fun titleOf(targetKey: String): String? = activePost(targetKey)?.title

    private fun activePost(targetKey: String): GlowCommunityPost? =
        postIdOf(targetKey)?.let(glowCommunityPostPort::findActiveById)

    private fun lockedPost(targetKey: String): GlowCommunityPost? =
        postIdOf(targetKey)?.let(glowCommunityPostPort::findActiveByIdForUpdate)

    private fun postIdOf(targetKey: String): Long? = targetKey.trim().toLongOrNull()
}
