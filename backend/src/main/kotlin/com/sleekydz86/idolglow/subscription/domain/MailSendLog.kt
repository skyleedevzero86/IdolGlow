package com.sleekydz86.idolglow.subscription.domain

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
@Table(name = "mail_send_logs")
class MailSendLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(name = "mail_kind", nullable = false, length = 40)
    val mailKind: MailSendKind,
    @Column(name = "recipient_email", nullable = false, length = 255)
    val recipientEmail: String,
    @Column(nullable = false, length = 500)
    val subject: String,
    @Column(name = "html_body", columnDefinition = "TEXT")
    val htmlBody: String? = null,
    @Column(name = "text_body", columnDefinition = "TEXT")
    val textBody: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: MailSendStatus,
    @Column(length = 500)
    val error: String? = null,
    @Column(name = "dispatch_id")
    val dispatchId: Long? = null,
    @Column(name = "sent_at")
    val sentAt: LocalDateTime? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
) {
    companion object {
        const val RECIPIENT_MAX_LENGTH = 255
        const val SUBJECT_MAX_LENGTH = 500
        const val ERROR_MAX_LENGTH = 500
        const val AUTH_BODY_NOTICE = "인증 메일 본문은 보안을 위해 저장하지 않습니다."

        @Suppress("LongParameterList")
        fun record(
            kind: MailSendKind,
            recipientEmail: String,
            subject: String,
            htmlBody: String?,
            textBody: String?,
            status: MailSendStatus,
            error: String?,
            dispatchId: Long?,
            now: LocalDateTime,
        ): MailSendLog =
            MailSendLog(
                mailKind = kind,
                recipientEmail = recipientEmail.trim().take(RECIPIENT_MAX_LENGTH),
                subject = subject.trim().take(SUBJECT_MAX_LENGTH),
                htmlBody = safeBody(kind, htmlBody),
                textBody = safeBody(kind, textBody),
                status = status,
                error = safeError(error),
                dispatchId = dispatchId,
                sentAt = now.takeIf { status == MailSendStatus.SENT },
                createdAt = now,
            )

        private fun safeBody(
            kind: MailSendKind,
            body: String?,
        ): String? =
            when {
                body.isNullOrEmpty() -> null
                !kind.keepsBody() -> AUTH_BODY_NOTICE
                else -> MailContentMasker.maskTokens(body)
            }

        private fun safeError(error: String?): String? =
            error
                ?.let(MailContentMasker::maskTokens)
                ?.trim()
                ?.take(ERROR_MAX_LENGTH)
                ?.takeIf { it.isNotEmpty() }
    }
}
