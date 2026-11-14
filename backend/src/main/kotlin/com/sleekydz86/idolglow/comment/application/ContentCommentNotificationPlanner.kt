package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.notification.domain.NotificationType

data class CommentNotificationPlan(
    val recipientId: Long,
    val type: NotificationType,
)

object ContentCommentNotificationPlanner {
    private const val LINK_LIMIT = 255
    private const val TITLE_PREVIEW_LENGTH = 20

    fun plan(
        actorId: Long,
        parentAuthorId: Long?,
        ownerId: Long?,
        adminIds: Set<Long>,
    ): List<CommentNotificationPlan> {
        if (actorId in adminIds) return emptyList()
        val plans = linkedMapOf<Long, NotificationType>()
        parentAuthorId?.let { plans[it] = NotificationType.REPLY_TO_MY_COMMENT }
        ownerId?.let { plans.putIfAbsent(it, NotificationType.COMMENT_ON_MY_POST) }
        return plans
            .filterKeys { it != actorId && it !in adminIds }
            .map { (recipientId, type) -> CommentNotificationPlan(recipientId, type) }
    }

    fun link(
        targetPath: String?,
        commentId: Long,
    ): String? = targetPath?.let { "$it#comment-$commentId" }?.takeIf { it.length <= LINK_LIMIT }

    fun titlePreview(title: String): String {
        val trimmed = title.trim()
        return if (trimmed.length <= TITLE_PREVIEW_LENGTH) trimmed else trimmed.take(TITLE_PREVIEW_LENGTH) + "…"
    }
}
