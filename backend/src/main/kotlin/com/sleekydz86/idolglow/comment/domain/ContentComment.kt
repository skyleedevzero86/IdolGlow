package com.sleekydz86.idolglow.comment.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.global.support.ContentTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "content_comments")
class ContentComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    val targetType: ContentTargetType,
    @Column(name = "target_key", nullable = false, length = TARGET_KEY_MAX_LENGTH)
    val targetKey: String,
    @Column(name = "target_title", nullable = false, length = TARGET_TITLE_MAX_LENGTH)
    var targetTitle: String,
    @Column(name = "target_path", length = 500)
    var targetPath: String? = null,
    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    var contentMarkdown: String,
    @Column(name = "parent_id")
    val parentId: Long? = null,
    @Column(name = "root_id")
    val rootId: Long? = null,
    @Column(nullable = false)
    val depth: Int = 1,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isOwnedBy(userId: Long?): Boolean = userId != null && this.userId == userId

    fun isDeleted(): Boolean = deletedAt != null

    fun threadRootId(): Long = rootId ?: id

    fun belongsTo(
        targetType: ContentTargetType,
        targetKey: String,
    ): Boolean = this.targetType == targetType && this.targetKey == targetKey

    fun edit(contentMarkdown: String) {
        this.contentMarkdown = normalizeContent(contentMarkdown)
    }

    fun delete(now: LocalDateTime) {
        deletedAt = now
    }

    fun reply(
        userId: Long,
        targetTitle: String,
        targetPath: String?,
        contentMarkdown: String,
    ): ContentComment {
        require(!isDeleted()) { "삭제된 댓글에는 답글을 달 수 없습니다." }
        require(depth < MAX_DEPTH) { "답글은 ${MAX_DEPTH}단계까지만 달 수 있습니다." }
        return ContentComment(
            userId = userId,
            targetType = targetType,
            targetKey = targetKey,
            targetTitle = targetTitle,
            targetPath = targetPath ?: this.targetPath,
            contentMarkdown = normalizeContent(contentMarkdown),
            parentId = id,
            rootId = threadRootId(),
            depth = depth + 1,
        )
    }

    companion object {
        const val MAX_DEPTH = 5
        const val CONTENT_MAX_LENGTH = 5000
        const val TARGET_KEY_MAX_LENGTH = 200
        const val TARGET_TITLE_MAX_LENGTH = 300

        fun create(
            userId: Long,
            targetType: ContentTargetType,
            targetKey: String,
            targetTitle: String,
            targetPath: String?,
            contentMarkdown: String,
        ): ContentComment =
            ContentComment(
                userId = userId,
                targetType = targetType,
                targetKey = targetKey,
                targetTitle = targetTitle,
                targetPath = targetPath,
                contentMarkdown = normalizeContent(contentMarkdown),
            )

        fun normalizeContent(contentMarkdown: String): String {
            require(contentMarkdown.isNotBlank()) { "댓글 내용을 입력해 주세요." }
            require(contentMarkdown.length <= CONTENT_MAX_LENGTH) { "댓글은 ${CONTENT_MAX_LENGTH}자 이하여야 합니다." }
            return contentMarkdown
        }
    }
}
