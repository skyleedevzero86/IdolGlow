package com.sleekydz86.idolglow.comment.application

import com.sleekydz86.idolglow.comment.application.dto.ContentCommentResult
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentTarget
import com.sleekydz86.idolglow.comment.application.dto.ContentCommentViewer
import com.sleekydz86.idolglow.comment.application.dto.CreateContentCommentCommand
import com.sleekydz86.idolglow.comment.application.dto.MyReviewResult
import com.sleekydz86.idolglow.comment.application.event.ContentCommentCreatedEvent
import com.sleekydz86.idolglow.comment.application.port.`in`.ContentCommentUseCase
import com.sleekydz86.idolglow.comment.application.port.out.CommentTargetPolicy
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentAuthorPort
import com.sleekydz86.idolglow.comment.application.port.out.ContentCommentPort
import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.comment.ContentCommentExceptionType
import com.sleekydz86.idolglow.global.support.ContentTargetPath
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ContentCommentService(
    private val contentCommentPort: ContentCommentPort,
    private val contentCommentAuthorPort: ContentCommentAuthorPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
    targetPolicies: List<CommentTargetPolicy>,
) : ContentCommentUseCase {
    private val policies: Map<ContentTargetType, CommentTargetPolicy> = targetPolicies.associateBy { it.targetType }

    @Transactional(readOnly = true)
    override fun findComments(
        target: ContentCommentTarget,
        viewer: ContentCommentViewer,
        request: OffsetPageRequest,
        latestFirst: Boolean,
    ): OffsetPageResult<ContentCommentResult> {
        val normalizedTarget = target.copy(key = normalizeKey(target.key))
        policies[normalizedTarget.type]?.checkReadable(normalizedTarget.key, viewer.userId, viewer.admin)
        val normalized = OffsetPageRequest.of(request.page, request.size)
        val roots = contentCommentPort.findThreadRoots(normalizedTarget, normalized, latestFirst)
        val replies =
            roots.items
                .takeIf { it.isNotEmpty() }
                ?.let { items -> contentCommentPort.findRepliesByRootIds(items.map { it.id }) }
                .orEmpty()
        val assembler = assemblerFor(roots.items + replies, viewer.userId)
        return OffsetPageResult.of(assembler.assemble(roots.items, replies), normalized, roots.totalElements)
    }

    override fun create(command: CreateContentCommentCommand): ContentCommentResult {
        val key = normalizeKey(command.targetKey)
        val path = ContentTargetPath.normalize(command.targetPath)
        val title =
            command.targetTitle
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(ContentComment.TARGET_TITLE_MAX_LENGTH)
                ?: (path ?: key).take(ContentComment.TARGET_TITLE_MAX_LENGTH)
        val policy = policies[command.targetType]
        policy?.checkWritable(key, command.userId)
        val parent = command.parentId?.let { loadParent(it, command.targetType, key) }
        val comment =
            parent?.reply(command.userId, title, path, command.contentMarkdown)
                ?: ContentComment.create(command.userId, command.targetType, key, title, path, command.contentMarkdown)
        val saved = contentCommentPort.save(comment)
        policy?.commentAdded(key)
        applicationEventPublisher.publishEvent(
            ContentCommentCreatedEvent(
                commentId = saved.id,
                actorUserId = command.userId,
                targetType = command.targetType,
                targetKey = key,
                targetTitle = title,
                targetPath = saved.targetPath,
                parentAuthorId = parent?.userId,
            ),
        )
        return assemblerFor(listOf(saved), command.userId).toResult(saved)
    }

    override fun update(
        commentId: Long,
        userId: Long,
        contentMarkdown: String,
    ): ContentCommentResult {
        val comment = loadActive(commentId)
        if (!comment.isOwnedBy(userId)) throw CustomException(ContentCommentExceptionType.NOT_COMMENT_OWNER)
        comment.edit(contentMarkdown)
        val saved = contentCommentPort.save(comment)
        return assemblerFor(listOf(saved), userId).toResult(saved)
    }

    override fun delete(
        commentId: Long,
        viewer: ContentCommentViewer,
    ) {
        val comment = loadActive(commentId)
        if (!comment.isOwnedBy(viewer.userId) && !viewer.admin) {
            throw CustomException(ContentCommentExceptionType.NOT_COMMENT_OWNER)
        }
        comment.delete(LocalDateTime.now())
        contentCommentPort.save(comment)
        policies[comment.targetType]?.commentRemoved(comment.targetKey)
    }

    @Transactional(readOnly = true)
    override fun findMyReviews(
        userId: Long,
        request: OffsetPageRequest,
    ): OffsetPageResult<MyReviewResult> {
        val normalized = OffsetPageRequest.of(request.page, request.size)
        val slice = contentCommentPort.findActiveByUserId(userId, normalized)
        val parentIds = slice.items.mapNotNull { it.parentId }.toSet()
        val parents = contentCommentPort.findAllByIds(parentIds).associateBy { it.id }
        val items =
            slice.items.map { comment ->
                MyReviewResult(
                    commentId = comment.id,
                    targetType = comment.targetType.name,
                    targetKey = comment.targetKey,
                    targetTitle = comment.targetTitle,
                    targetPath = comment.targetPath,
                    contentMarkdown = comment.contentMarkdown,
                    excerpt = MarkdownExcerpt.of(comment.contentMarkdown),
                    parentId = comment.parentId,
                    parentExcerpt = comment.parentId?.let { parents[it] }?.let(::parentExcerptOf),
                    depth = comment.depth,
                    createdAt = comment.createdAt,
                    updatedAt = comment.updatedAt,
                )
            }
        return OffsetPageResult.of(items, normalized, slice.totalElements)
    }

    private fun parentExcerptOf(parent: ContentComment): String =
        if (parent.isDeleted()) {
            ContentCommentTreeAssembler.DELETED_PLACEHOLDER
        } else {
            MarkdownExcerpt.of(parent.contentMarkdown)
        }

    private fun loadParent(
        parentId: Long,
        targetType: ContentTargetType,
        targetKey: String,
    ): ContentComment {
        val parent =
            contentCommentPort.findById(parentId)
                ?: throw IllegalArgumentException("답글을 달 댓글을 찾을 수 없습니다.")
        require(parent.belongsTo(targetType, targetKey)) { "다른 콘텐츠의 댓글에는 답글을 달 수 없습니다." }
        return parent
    }

    private fun loadActive(commentId: Long): ContentComment =
        contentCommentPort.findById(commentId)?.takeUnless { it.isDeleted() }
            ?: throw CustomException(ContentCommentExceptionType.COMMENT_NOT_FOUND)

    private fun assemblerFor(
        comments: List<ContentComment>,
        viewerId: Long?,
    ): ContentCommentTreeAssembler {
        val userIds = comments.filterNot { it.isDeleted() }.map { it.userId }.toSet()
        val nicknames = if (userIds.isEmpty()) emptyMap() else contentCommentAuthorPort.findNicknames(userIds)
        return ContentCommentTreeAssembler(nicknames, viewerId)
    }

    private fun normalizeKey(raw: String): String {
        val key = raw.trim()
        require(key.isNotEmpty()) { "대상 키를 입력해 주세요." }
        require(key.length <= ContentComment.TARGET_KEY_MAX_LENGTH) {
            "대상 키는 ${ContentComment.TARGET_KEY_MAX_LENGTH}자 이하여야 합니다."
        }
        return key
    }
}
