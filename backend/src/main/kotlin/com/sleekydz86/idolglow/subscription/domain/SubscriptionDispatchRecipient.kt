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
    name = "subscription_dispatch_recipients",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_subscription_dispatch_recipient",
            columnNames = ["dispatch_id", "email"],
        ),
    ],
)
class SubscriptionDispatchRecipient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "dispatch_id", nullable = false)
    val dispatchId: Long,
    @Column(nullable = false, length = 255)
    val email: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SubscriptionDispatchRecipientStatus,
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
    @Column(name = "error", length = 1000)
    var errorMessage: String? = null,
) : BaseEntity() {
    fun applyOutcome(
        sent: Boolean,
        error: String?,
        sentAt: LocalDateTime,
    ) {
        if (sent) {
            status = SubscriptionDispatchRecipientStatus.SENT
            this.sentAt = sentAt
            errorMessage = null
            return
        }
        status = SubscriptionDispatchRecipientStatus.FAILED
        this.sentAt = null
        errorMessage = error?.trim()?.take(ERROR_MAX_LENGTH)?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val ERROR_MAX_LENGTH: Int = 1000

        fun create(
            dispatchId: Long,
            email: String,
        ): SubscriptionDispatchRecipient {
            require(dispatchId > 0) { "발송 이력 ID는 0보다 커야 합니다. dispatchId=$dispatchId" }
            require(email.isNotBlank()) { "수신 이메일은 비울 수 없습니다." }
            return SubscriptionDispatchRecipient(
                dispatchId = dispatchId,
                email = email.trim(),
                status = SubscriptionDispatchRecipientStatus.FAILED,
            )
        }
    }
}
