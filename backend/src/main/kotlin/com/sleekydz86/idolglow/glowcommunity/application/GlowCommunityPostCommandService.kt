package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType
import com.sleekydz86.idolglow.glowcommunity.application.dto.CreateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityLikeResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostResult
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.application.dto.UpdateGlowCommunityPostCommand
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityPostCommandUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityLike
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityReport
import com.sleekydz86.idolglow.notification.application.AdminNotificationPublisher
import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class GlowCommunityPostCommandService(
    private val glowCommunityPostPort: GlowCommunityPostPort,
    private val glowCommunityReactionPort: GlowCommunityReactionPort,
    private val glowCommunityPostAssembler: GlowCommunityPostAssembler,
    private val adminNotificationPublisher: AdminNotificationPublisher,
) : GlowCommunityPostCommandUseCase {
    override fun create(command: CreateGlowCommunityPostCommand): GlowCommunityPostResult {
        val post =
            glowCommunityPostPort.save(
                GlowCommunityPost.create(
                    userId = command.userId,
                    tag = command.tag,
                    title = command.title,
                    contentMarkdown = command.content,
                ),
            )
        return glowCommunityPostAssembler.toResult(post, GlowCommunityViewer.member(command.userId))
    }

    override fun update(command: UpdateGlowCommunityPostCommand): GlowCommunityPostResult {
        val post = loadOwnedPost(command.postId, command.userId)
        post.edit(tag = command.tag, title = command.title, contentMarkdown = command.content)
        val saved = glowCommunityPostPort.save(post)
        return glowCommunityPostAssembler.toResult(saved, GlowCommunityViewer.member(command.userId))
    }

    override fun delete(
        postId: Long,
        userId: Long,
    ) {
        val post = loadOwnedPost(postId, userId)
        post.delete(LocalDateTime.now())
        glowCommunityPostPort.save(post)
    }

    override fun toggleLike(
        postId: Long,
        userId: Long,
    ): GlowCommunityLikeResult {
        val post = loadVisiblePostForUpdate(postId, userId)
        if (post.blinded) throw CustomException(GlowCommunityExceptionType.BLINDED_POST)
        val existing = glowCommunityReactionPort.findLike(postId, userId)
        val liked =
            if (existing != null) {
                glowCommunityReactionPort.deleteLike(existing)
                post.adjustLikeCount(-1L)
                false
            } else {
                glowCommunityReactionPort.saveLike(GlowCommunityLike(postId = postId, userId = userId))
                post.adjustLikeCount(1L)
                true
            }
        glowCommunityPostPort.save(post)
        return GlowCommunityLikeResult(liked = liked, likeCount = post.likeCount)
    }

    override fun report(
        postId: Long,
        userId: Long,
        reason: String?,
    ) {
        val post = loadVisiblePostForUpdate(postId, userId)
        if (post.isOwnedBy(userId)) throw CustomException(GlowCommunityExceptionType.CANNOT_REPORT_OWN_POST)
        if (glowCommunityReactionPort.existsReport(postId, userId)) return
        glowCommunityReactionPort.saveReport(GlowCommunityReport.create(postId, userId, reason))
        val autoBlinded = post.addReport(LocalDateTime.now())
        glowCommunityPostPort.save(post)
        notifyAdminsOfReport(post, autoBlinded)
    }

    private fun notifyAdminsOfReport(
        post: GlowCommunityPost,
        autoBlinded: Boolean,
    ) {
        val title = titlePreview(post.title)
        if (post.reportCount == 1L) {
            adminNotificationPublisher.publish(
                AdminNotificationRequestedEvent(
                    type = NotificationType.ADMIN_POST_REPORTED,
                    title = "게시글 신고",
                    message = "게시글 '$title'이 신고되었어요.",
                    link = ADMIN_REPORTS_LINK,
                ),
            )
        }
        if (autoBlinded) {
            adminNotificationPublisher.publish(
                AdminNotificationRequestedEvent(
                    type = NotificationType.ADMIN_POST_AUTO_BLINDED,
                    title = "게시글 자동 블라인드",
                    message = "게시글 '$title'이 신고 ${GlowCommunityPost.AUTO_BLIND_REPORT_COUNT}건으로 자동 블라인드되었어요.",
                    link = ADMIN_REPORTS_LINK,
                ),
            )
        }
    }

    private fun titlePreview(title: String): String =
        if (title.length <= TITLE_PREVIEW_LENGTH) title else title.take(TITLE_PREVIEW_LENGTH) + "…"

    private fun loadOwnedPost(
        postId: Long,
        userId: Long,
    ): GlowCommunityPost {
        val post =
            glowCommunityPostPort.findActiveById(postId)
                ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)
        if (!post.isOwnedBy(userId)) throw CustomException(GlowCommunityExceptionType.NOT_POST_OWNER)
        return post
    }

    private fun loadVisiblePostForUpdate(
        postId: Long,
        userId: Long,
    ): GlowCommunityPost =
        glowCommunityPostPort
            .findActiveByIdForUpdate(postId)
            ?.takeIf { it.visibleTo(userId, admin = false) }
            ?: throw CustomException(GlowCommunityExceptionType.POST_NOT_FOUND)

    companion object {
        const val ADMIN_REPORTS_LINK = "/admin/glow-community/reports"
        private const val TITLE_PREVIEW_LENGTH = 20
    }
}
