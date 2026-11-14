package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.event.ContentCommentCreatedEvent
import com.sleekydz86.idolglow.comment.application.port.out.CommentTargetPolicy
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentAuthorPort
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ContentCommentNotificationTxService(
    private val contentCommentAuthorPort: ContentCommentAuthorPort,
    private val notificationCommandService: NotificationCommandService,
    private val targetPolicies: List<CommentTargetPolicy>,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun notify(event: ContentCommentCreatedEvent): Int {
        val policy = targetPolicies.firstOrNull { it.targetType == event.targetType }
        val ownerId = policy?.ownerOf(event.targetKey)
        val candidates = setOfNotNull(event.actorUserId, event.parentAuthorId, ownerId)
        val plans =
            ContentCommentNotificationPlanner.plan(
                actorId = event.actorUserId,
                parentAuthorId = event.parentAuthorId,
                ownerId = ownerId,
                adminIds = contentCommentAuthorPort.findAdminIds(candidates),
            )
        if (plans.isEmpty()) return 0
        val actorName =
            contentCommentAuthorPort.findNicknames(listOf(event.actorUserId))[event.actorUserId] ?: UNKNOWN_ACTOR
        val targetTitle = policy?.titleOf(event.targetKey) ?: event.targetTitle
        val title = ContentCommentNotificationPlanner.titlePreview(targetTitle)
        val link = ContentCommentNotificationPlanner.link(event.targetPath, event.commentId)
        return plans.count { plan ->
            val (heading, message) =
                when (plan.type) {
                    NotificationType.REPLY_TO_MY_COMMENT -> "새 답글" to "${actorName}님이 내 댓글에 답글을 남겼어요."
                    else -> "새 댓글" to "${actorName}님이 내 게시글 '$title'에 댓글을 남겼어요."
                }
            notificationCommandService.create(
                userId = plan.recipientId,
                type = plan.type,
                title = heading,
                message = message,
                link = link,
            ) != null
        }
    }

    companion object {
        private const val UNKNOWN_ACTOR = "누군가"
    }
}
