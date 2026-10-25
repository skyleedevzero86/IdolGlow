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
    ],
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
    var dispatchStatus: SubscriptionDispatchStatus,
    @Column(name = "recipient_count", nullable = false)
    var recipientCount: Long,
    @Column(name = "success_count", nullable = false)
    var successCount: Long = 0,
    @Column(name = "failure_count", nullable = false)
    var failureCount: Long = 0,
    @Column(name = "last_attempt_at")
    var lastAttemptAt: LocalDateTime? = null,
    @Column(name = "content_created_at")
    val contentCreatedAt: LocalDateTime? = null,
    @Column(name = "dispatched_at", nullable = false)
    val dispatchedAt: LocalDateTime,
) : BaseEntity() {
    fun beginAttempt(at: LocalDateTime) {
        lastAttemptAt = at
    }

    fun isStaleSending(staleBefore: LocalDateTime): Boolean {
        val attemptAt = lastAttemptAt
        return dispatchStatus == SubscriptionDispatchStatus.SENDING &&
            (attemptAt == null || !attemptAt.isAfter(staleBefore))
    }

    fun complete(
        status: SubscriptionDispatchStatus,
        recipientCount: Long,
        successCount: Long,
        failureCount: Long,
    ) {
        require(dispatchStatus == SubscriptionDispatchStatus.SENDING) {
            "발송 중 상태에서만 마칠 수 있습니다. status=$dispatchStatus"
        }
        require(status != SubscriptionDispatchStatus.SENDING) {
            "발송 종료 상태가 아닙니다. status=$status"
        }
        require(recipientCount >= 0 && successCount >= 0 && failureCount >= 0) {
            "발송 건수는 0 이상이어야 합니다. recipient=$recipientCount success=$successCount failure=$failureCount"
        }
        this.dispatchStatus = status
        this.recipientCount = recipientCount
        this.successCount = successCount
        this.failureCount = failureCount
    }

    companion object {
        const val RESUME_AFTER_MINUTES: Long = 15

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
                lastAttemptAt = dispatchedAt,
                contentCreatedAt = contentCreatedAt,
                dispatchedAt = dispatchedAt,
            )
        }
    }
}
