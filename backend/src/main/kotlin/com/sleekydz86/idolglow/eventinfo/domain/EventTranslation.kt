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
    title: String,
    synopsis: String? = null,
    origin: EventTranslationOrigin,
) : BaseEntity() {
    @Column(nullable = false, length = 255)
    var title: String = title
        protected set

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "synopsis")
    var synopsis: String? = synopsis
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var origin: EventTranslationOrigin = origin
        protected set

    fun offer(
        incoming: EventTranslationOrigin,
        title: String,
        synopsis: String?,
    ): Boolean {
        if (!incoming.canReplace(origin)) return false
        if (incoming == origin && this.title == title && this.synopsis == synopsis) return false
        this.origin = incoming
        this.title = title
        this.synopsis = synopsis
        return true
    }

    companion object {
        const val TITLE_MAX: Int = 255

        fun provider(
            globalId: String,
            locale: String,
            title: String,
            synopsis: String?,
        ): EventTranslation =
            create(globalId, EventLocale.normalize(locale), title.trim().take(TITLE_MAX), normalizeSynopsis(synopsis), EventTranslationOrigin.PROVIDER)

        fun machine(
            globalId: String,
            locale: String,
            title: String,
        ): EventTranslation = create(globalId, targetLocale(locale), requireWithinMax(title.trim(), locale), null, EventTranslationOrigin.MACHINE)

        fun manual(
            globalId: String,
            locale: String,
            title: String,
        ): EventTranslation = create(globalId, targetLocale(locale), requireWithinMax(title.trim(), locale), null, EventTranslationOrigin.MANUAL)

        private fun targetLocale(locale: String): String {
            val normalized = EventLocale.normalize(locale)
            require(normalized in EventTranslationLocales.TARGETS) {
                "번역 언어는 en, ja, zh 중 하나여야 합니다. locale=$normalized"
            }
            return normalized
        }

        fun requireWithinMax(
            title: String,
            locale: String,
        ): String {
            require(title.codePointCount(0, title.length) <= TITLE_MAX) {
                "공연 번역 제목은 ${TITLE_MAX}자 이하여야 합니다. locale=$locale"
            }
            return title
        }

        fun normalizeSynopsis(synopsis: String?): String? = synopsis?.trim()?.takeIf { it.isNotEmpty() }

        private fun create(
            globalId: String,
            locale: String,
            title: String,
            synopsis: String?,
            origin: EventTranslationOrigin,
        ): EventTranslation {
            require(globalId.isNotBlank()) { "글로벌 공연 ID는 비울 수 없습니다." }
            require(title.isNotBlank()) { "공연 번역 제목은 비울 수 없습니다. locale=$locale" }
            return EventTranslation(
                globalId = globalId.trim(),
                locale = locale,
                title = title,
                synopsis = synopsis,
                origin = origin,
            )
        }
    }
}
