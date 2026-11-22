package com.sleekydz86.idolglow.glowcommunity.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
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
@Table(name = "glow_community_posts")
class GlowCommunityPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tag: GlowCommunityTag,
    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    var title: String,
    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    var contentMarkdown: String,
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,
    @Column(name = "comment_count", nullable = false)
    var commentCount: Long = 0L,
    @Column(name = "report_count", nullable = false)
    var reportCount: Long = 0L,
    @Column(name = "last_reported_at")
    var lastReportedAt: LocalDateTime? = null,
    @Column(nullable = false)
    var blinded: Boolean = false,
    @Column(name = "blinded_at")
    var blindedAt: LocalDateTime? = null,
    @Column(name = "blind_reason", length = BLIND_REASON_MAX_LENGTH)
    var blindReason: String? = null,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isDeleted(): Boolean = deletedAt != null

    fun delete(now: LocalDateTime) {
        deletedAt = now
    }

    fun isOwnedBy(userId: Long?): Boolean = userId != null && this.userId == userId

    fun visibleTo(
        viewerId: Long?,
        admin: Boolean,
    ): Boolean = !blinded || admin || isOwnedBy(viewerId)

    fun edit(
        tag: GlowCommunityTag,
        title: String,
        contentMarkdown: String,
    ) {
        this.tag = tag
        this.title = normalizeTitle(title)
        this.contentMarkdown = normalizeContent(contentMarkdown)
    }

    fun adjustLikeCount(delta: Long) {
        likeCount = (likeCount + delta).coerceAtLeast(0L)
    }

    fun adjustCommentCount(delta: Long) {
        commentCount = (commentCount + delta).coerceAtLeast(0L)
    }

    fun addReport(now: LocalDateTime): Boolean {
        reportCount += 1
        lastReportedAt = now
        if (blinded || reportCount != AUTO_BLIND_REPORT_COUNT) return false
        blind(now, AUTO_BLIND_REASON)
        return true
    }

    fun blind(
        now: LocalDateTime,
        reason: String,
    ) {
        blinded = true
        blindedAt = now
        blindReason = reason.trim().ifEmpty { ADMIN_BLIND_REASON }.take(BLIND_REASON_MAX_LENGTH)
    }

    fun unblind() {
        blinded = false
        blindedAt = null
        blindReason = null
    }

    companion object {
        const val TITLE_MAX_LENGTH = 150
        const val CONTENT_MAX_LENGTH = 20000
        const val BLIND_REASON_MAX_LENGTH = 100
        const val AUTO_BLIND_REPORT_COUNT = 3L
        const val AUTO_BLIND_REASON = "신고 누적 자동 블라인드"
        const val ADMIN_BLIND_REASON = "관리자 블라인드"

        fun create(
            userId: Long,
            tag: GlowCommunityTag,
            title: String,
            contentMarkdown: String,
        ): GlowCommunityPost =
            GlowCommunityPost(
                userId = userId,
                tag = tag,
                title = normalizeTitle(title),
                contentMarkdown = normalizeContent(contentMarkdown),
            )

        private fun normalizeTitle(title: String): String {
            val trimmed = title.trim()
            require(trimmed.isNotEmpty()) { "제목을 입력해 주세요." }
            require(trimmed.length <= TITLE_MAX_LENGTH) { "제목은 ${TITLE_MAX_LENGTH}자 이하여야 합니다." }
            return trimmed
        }

        private fun normalizeContent(contentMarkdown: String): String {
            require(contentMarkdown.isNotBlank()) { "본문을 입력해 주세요." }
            require(contentMarkdown.length <= CONTENT_MAX_LENGTH) { "본문은 ${CONTENT_MAX_LENGTH}자 이하여야 합니다." }
            return contentMarkdown
        }
    }
}
