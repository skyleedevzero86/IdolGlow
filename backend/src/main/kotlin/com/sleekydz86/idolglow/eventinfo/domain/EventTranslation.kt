package com.sleekydz86.idolglow.eventinfo.domain

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "event_translation",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_translation_global_locale", columnNames = ["global_id", "locale"]),
    ],
)
class EventTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "global_id", nullable = false, length = 36)
    val globalId: String,
    @Column(nullable = false, length = 8)
    val locale: String,
    @Column(nullable = false, length = 255)
    var title: String,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "synopsis")
    var synopsis: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val origin: EventTranslationOrigin,
) : BaseEntity() {
    fun replaceProviderText(
        title: String,
        synopsis: String?,
    ) {
        require(origin == EventTranslationOrigin.PROVIDER) {
            "공급처 번역만 갱신할 수 있습니다. locale=$locale origin=$origin"
        }
        this.title = title
        this.synopsis = synopsis
    }

    companion object {
        const val TITLE_MAX: Int = 255

        fun provider(
            globalId: String,
            locale: String,
            title: String,
            synopsis: String?,
        ): EventTranslation {
            require(globalId.isNotBlank()) { "글로벌 공연 ID는 비울 수 없습니다." }
            require(title.isNotBlank()) { "공연 번역 제목은 비울 수 없습니다. locale=$locale" }
            return EventTranslation(
                globalId = globalId.trim(),
                locale = EventLocale.normalize(locale),
                title = title.trim().take(TITLE_MAX),
                synopsis = synopsis?.trim()?.takeIf { it.isNotEmpty() },
                origin = EventTranslationOrigin.PROVIDER,
            )
        }
    }
}
