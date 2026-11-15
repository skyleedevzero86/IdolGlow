package com.sleekydz86.idolglow.translation.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Duration
import java.time.LocalDateTime

@Entity
@Table(
    name = "content_translation",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_content_translation_target",
            columnNames = ["content_type", "content_key", "locale"],
        ),
    ],
    indexes = [
        Index(name = "idx_content_translation_due", columnList = "status, next_attempt_at"),
    ],
)
class ContentTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    val contentType: TranslationContentType,
    @Column(name = "content_key", nullable = false, length = 120)
    val contentKey: String,
    @Column(nullable = false, length = 8)
    val locale: String,
    @Column(name = "source_hash", nullable = false, length = 64)
    var sourceHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TranslationStatus = TranslationStatus.PENDING,
    @Column(nullable = false)
    var priority: Int = 0,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "fields_json")
    var fieldsJson: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var origin: TranslationOrigin? = null,
    @Column(length = 80)
    var model: String? = null,
    @Column(name = "prompt_version", length = 20)
    var promptVersion: String? = null,
    @Column(name = "check_notes", length = 1000)
    var checkNotes: String? = null,
    @Column(nullable = false)
    var attempts: Int = 0,
    @Column(name = "next_attempt_at")
    var nextAttemptAt: LocalDateTime? = null,
    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0,
    @Column(name = "translated_at")
    var translatedAt: LocalDateTime? = null,
    @Column(name = "reviewed_by")
    var reviewedBy: Long? = null,
    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,
) : BaseEntity() {
    val servable: Boolean
        get() = status == TranslationStatus.VISIBLE && !fieldsJson.isNullOrBlank()

    fun resetFor(
        hash: String,
        newPriority: Int,
    ) {
        sourceHash = hash
        status = TranslationStatus.PENDING
        priority = maxOf(priority, newPriority)
        fieldsJson = null
        origin = null
        model = null
        promptVersion = null
        checkNotes = null
        attempts = 0
        nextAttemptAt = null
        reportCount = 0
        translatedAt = null
        reviewedBy = null
        reviewedAt = null
    }

    fun raisePriority(newPriority: Int) {
        priority = maxOf(priority, newPriority)
    }

    fun rehash(hash: String) {
        sourceHash = hash
    }

    fun markTranslated(
        fields: String,
        next: TranslationStatus,
        modelName: String,
        version: String,
        notes: String?,
        now: LocalDateTime,
    ) {
        require(next == TranslationStatus.VISIBLE || next == TranslationStatus.REVIEW) {
            "번역 결과 상태가 올바르지 않습니다. status=$next"
        }
        fieldsJson = fields
        status = next
        origin = TranslationOrigin.MACHINE
        model = modelName.take(MODEL_MAX)
        promptVersion = version.take(VERSION_MAX)
        checkNotes = notes?.take(NOTES_MAX)
        translatedAt = now
        nextAttemptAt = null
    }

    fun markRetry(
        note: String,
        now: LocalDateTime,
        maxAttempts: Int,
    ) {
        attempts += 1
        checkNotes = note.take(NOTES_MAX)
        if (attempts >= maxAttempts) {
            status = TranslationStatus.FAILED
            nextAttemptAt = null
            return
        }
        val backoff = BASE_BACKOFF.multipliedBy(1L shl (attempts - 1)).coerceAtMost(MAX_BACKOFF)
        nextAttemptAt = now.plus(backoff)
    }

    fun holdForReview(note: String) {
        status = TranslationStatus.REVIEW
        checkNotes = note.take(NOTES_MAX)
        nextAttemptAt = null
    }

    fun skip(note: String) {
        status = TranslationStatus.SKIPPED
        checkNotes = note.take(NOTES_MAX)
        nextAttemptAt = null
    }

    fun approve(
        adminId: Long,
        now: LocalDateTime,
    ) {
        require(status == TranslationStatus.REVIEW) { "검수 대기 번역만 승인할 수 있습니다. status=$status" }
        require(!fieldsJson.isNullOrBlank()) { "번역 내용이 없어 승인할 수 없습니다. id=$id" }
        status = TranslationStatus.VISIBLE
        reviewedBy = adminId
        reviewedAt = now
    }

    fun editManually(
        fields: String,
        adminId: Long,
        now: LocalDateTime,
    ) {
        fieldsJson = fields
        status = TranslationStatus.VISIBLE
        origin = TranslationOrigin.MANUAL
        model = null
        promptVersion = null
        checkNotes = null
        reportCount = 0
        reviewedBy = adminId
        reviewedAt = now
        translatedAt = now
    }

    fun requeue() {
        status = TranslationStatus.PENDING
        attempts = 0
        nextAttemptAt = null
        checkNotes = null
    }

    fun report(threshold: Int): Boolean {
        reportCount += 1
        if (status == TranslationStatus.VISIBLE && reportCount >= threshold) {
            holdForReview("reported:$reportCount")
            return true
        }
        return false
    }

    companion object {
        private const val MODEL_MAX = 80
        private const val VERSION_MAX = 20
        private const val NOTES_MAX = 1000
        private val BASE_BACKOFF: Duration = Duration.ofMinutes(5)
        private val MAX_BACKOFF: Duration = Duration.ofHours(6)

        fun pending(
            type: TranslationContentType,
            key: String,
            locale: String,
            hash: String,
            priority: Int,
        ): ContentTranslation =
            ContentTranslation(
                contentType = type,
                contentKey = key,
                locale = TranslationLocales.requireTarget(locale),
                sourceHash = hash,
                priority = priority,
            )
    }
}
