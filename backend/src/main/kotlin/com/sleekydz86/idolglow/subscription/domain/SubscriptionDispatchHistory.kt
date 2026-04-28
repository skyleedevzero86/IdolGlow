package com.sleekydz86.idolglow.subscription.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "subscription_dispatch_histories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_subscription_dispatch_content",
            columnNames = ["content_type", "content_slug", "dispatch_channel"],
        ),
    ]
)
class SubscriptionDispatchHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    val contentType: SubscriptionContentType,

    @Column(name = "content_slug", nullable = false, length = 160)
    val contentSlug: String,

    @Column(name = "content_title", nullable = false, length = 255)
    val contentTitle: String,

    @Column(name = "content_summary", length = 1000)
    val contentSummary: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_channel", nullable = false, length = 20)
    val dispatchChannel: SubscriptionDispatchChannel,

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 20)
    val dispatchStatus: SubscriptionDispatchStatus,

    @Column(name = "recipient_count", nullable = false)
    val recipientCount: Long,

    @Column(name = "content_created_at")
    val contentCreatedAt: LocalDateTime? = null,

    @Column(name = "dispatched_at", nullable = false)
    val dispatchedAt: LocalDateTime,
) : BaseEntity() {

    companion object {
        fun record(
            contentType: SubscriptionContentType,
            contentSlug: String,
            contentTitle: String,
            contentSummary: String?,
            recipientCount: Long,
            contentCreatedAt: LocalDateTime?,
            dispatchedAt: LocalDateTime,
            dispatchStatus: SubscriptionDispatchStatus = SubscriptionDispatchStatus.RECORDED,
        ): SubscriptionDispatchHistory {
            require(contentSlug.isNotBlank()) { "발송 콘텐츠 슬러그는 비울 수 없습니다." }
            require(contentTitle.isNotBlank()) { "발송 콘텐츠 제목은 비울 수 없습니다." }
            require(recipientCount >= 0) { "수신자 수는 0 이상이어야 합니다." }

            return SubscriptionDispatchHistory(
                contentType = contentType,
                contentSlug = contentSlug.trim(),
                contentTitle = contentTitle.trim(),
                contentSummary = contentSummary?.trim()?.takeIf { it.isNotEmpty() },
                dispatchChannel = SubscriptionDispatchChannel.EMAIL,
                dispatchStatus = dispatchStatus,
                recipientCount = recipientCount,
                contentCreatedAt = contentCreatedAt,
                dispatchedAt = dispatchedAt,
            )
        }
    }
}

enum class SubscriptionContentType(val label: String, val audience: SubscriptionAudience) {
    NEWSLETTER("뉴스레터", SubscriptionAudience.NEWSLETTER),
    WEBZINE_ISSUE("웹진", SubscriptionAudience.WEBZINE_ISSUE),
}

enum class SubscriptionDispatchChannel(val label: String) {
    EMAIL("이메일"),
}

enum class SubscriptionDispatchStatus(val label: String) {
    RECORDED("기록됨"),
    SENT("발송완료"),
    FAILED("발송실패"),
}
