package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentResult
import com.sleekydz86.idolglow.comment.domain.ContentComment

class ContentCommentTreeAssembler(
    private val nicknames: Map<Long, String>,
    private val viewerId: Long?,
) {
    fun assemble(
        roots: List<ContentComment>,
        replies: List<ContentComment>,
    ): List<ContentCommentResult> {
        val childrenByParentId = replies.groupBy { it.parentId }
        return roots.mapNotNull { build(it, childrenByParentId) }
    }

    fun toResult(
        comment: ContentComment,
        replies: List<ContentCommentResult> = emptyList(),
    ): ContentCommentResult =
        if (comment.isDeleted()) {
            ContentCommentResult(
                id = comment.id,
                targetType = comment.targetType.name,
                targetKey = comment.targetKey,
                parentId = comment.parentId,
                depth = comment.depth,
                contentMarkdown = DELETED_PLACEHOLDER,
                authorName = null,
                authorId = null,
                createdAt = comment.createdAt,
                updatedAt = null,
                mine = false,
                deleted = true,
                replies = replies,
            )
        } else {
            ContentCommentResult(
                id = comment.id,
                targetType = comment.targetType.name,
                targetKey = comment.targetKey,
                parentId = comment.parentId,
                depth = comment.depth,
                contentMarkdown = comment.contentMarkdown,
                authorName = nicknames[comment.userId] ?: UNKNOWN_AUTHOR,
                authorId = comment.userId,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                mine = comment.isOwnedBy(viewerId),
                deleted = false,
                replies = replies,
            )
        }

    private fun build(
        comment: ContentComment,
        childrenByParentId: Map<Long?, List<ContentComment>>,
    ): ContentCommentResult? {
        val replies =
            childrenByParentId[comment.id]
                .orEmpty()
                .sortedWith(compareBy<ContentComment>({ it.createdAt }, { it.id }))
                .mapNotNull { build(it, childrenByParentId) }
        if (comment.isDeleted() && replies.isEmpty()) return null
        return toResult(comment, replies)
    }

    companion object {
        const val DELETED_PLACEHOLDER = "삭제된 댓글입니다"
        const val UNKNOWN_AUTHOR = "알 수 없음"
    }
}
