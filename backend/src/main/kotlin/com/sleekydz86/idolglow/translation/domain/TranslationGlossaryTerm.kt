package com.sleekydz86.idolglow.translation.domain

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

@Entity
@Table(
    name = "translation_glossary_term",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_translation_glossary_term", columnNames = ["source_term", "locale"]),
    ],
)
class TranslationGlossaryTerm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "source_term", nullable = false, length = 100)
    val sourceTerm: String,
    @Column(nullable = false, length = 8)
    val locale: String,
    @Column(name = "target_term", length = 200)
    val targetTerm: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val mode: GlossaryMode,
) : BaseEntity() {
    fun expected(): String = if (mode == GlossaryMode.KEEP) sourceTerm else targetTerm.orEmpty()

    fun appliesTo(locale: String): Boolean = this.locale == TranslationLocales.ANY || this.locale == locale

    companion object {
        private const val TERM_MAX = 100
        private const val TARGET_MAX = 200

        fun fixed(
            sourceTerm: String,
            locale: String,
            targetTerm: String,
        ): TranslationGlossaryTerm {
            val source = sourceTerm.trim()
            val target = targetTerm.trim()
            require(source.isNotEmpty() && source.length <= TERM_MAX) { "용어는 1~${TERM_MAX}자여야 합니다." }
            require(target.isNotEmpty() && target.length <= TARGET_MAX) { "번역어는 1~${TARGET_MAX}자여야 합니다." }
            return TranslationGlossaryTerm(
                sourceTerm = source,
                locale = TranslationLocales.requireTarget(locale),
                targetTerm = target,
                mode = GlossaryMode.FIXED,
            )
        }

        fun keep(sourceTerm: String): TranslationGlossaryTerm {
            val source = sourceTerm.trim()
            require(source.isNotEmpty() && source.length <= TERM_MAX) { "용어는 1~${TERM_MAX}자여야 합니다." }
            return TranslationGlossaryTerm(
                sourceTerm = source,
                locale = TranslationLocales.ANY,
                targetTerm = null,
                mode = GlossaryMode.KEEP,
            )
        }
    }
}
