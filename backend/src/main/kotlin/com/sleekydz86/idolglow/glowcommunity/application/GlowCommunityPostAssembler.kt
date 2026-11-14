package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityAuthorPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import org.springframework.stereotype.Component

@Component
class GlowCommunityPostAssembler(
    private val glowCommunityAuthorPort: GlowCommunityAuthorPort,
    private val glowCommunityReactionPort: GlowCommunityReactionPort,
) {
    fun toResults(
        posts: List<GlowCommunityPost>,
        viewer: GlowCommunityViewer,
    ): List<GlowCommunityPostResult> {
        if (posts.isEmpty()) return emptyList()
        val nicknames = nicknamesOf(posts.map { it.userId })
        val likedPostIds =
            viewer.userId
                ?.let { glowCommunityReactionPort.findLikedPostIds(it, posts.map { post -> post.id }) }
                .orEmpty()
        return posts.map { post ->
            val liked = post.id in likedPostIds
            GlowCommunityPostResult(
                id = post.id,
                tag = post.tag.code,
                tagLabel = post.tag.label,
                title = post.title,
                content = post.contentMarkdown,
                authorName = nicknames[post.userId] ?: UNKNOWN_AUTHOR,
                authorId = post.userId,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                liked = liked,
                likedByMe = liked,
                mine = post.isOwnedBy(viewer.userId),
                blinded = post.blinded,
                blindReason = post.blindReason.takeIf { post.blinded },
            )
        }
    }

    fun toResult(
        post: GlowCommunityPost,
        viewer: GlowCommunityViewer,
    ): GlowCommunityPostResult = toResults(listOf(post), viewer).first()

    fun nicknamesOf(userIds: Collection<Long>): Map<Long, String> =
        if (userIds.isEmpty()) emptyMap() else glowCommunityAuthorPort.findNicknames(userIds.toSet())

    companion object {
        const val UNKNOWN_AUTHOR = "알 수 없음"
    }
}
