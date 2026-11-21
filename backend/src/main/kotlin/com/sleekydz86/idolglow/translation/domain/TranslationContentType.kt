package com.sleekydz86.idolglow.translation.domain

import java.util.Locale

enum class TranslationContentType(
    val official: Boolean,
    val fields: List<String>,
) {
    NOTICE(true, listOf("title", "introduction", "body")),
    EVENT(true, listOf("title", "introduction", "body")),
    PRODUCT(true, listOf("name", "description")),
    ARTICLE(true, listOf("title", "kicker", "summary")),
    GLOW_COMMUNITY_POST(false, listOf("title", "content")),
    CONTENT_COMMENT(false, listOf("content")),
    ;

    companion object {
        private const val ECHO_LIMIT = 40

        fun parse(raw: String?): TranslationContentType {
            val normalized =
                raw
                    ?.trim()
                    ?.uppercase(Locale.ROOT)
                    ?.replace('-', '_')
                    .orEmpty()
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("번역할 수 없는 콘텐츠 유형입니다: ${normalized.take(ECHO_LIMIT)}")
        }
    }
}

enum class TranslationStatus {
    PENDING,
    VISIBLE,
    REVIEW,
    FAILED,
    SKIPPED,
}

enum class TranslationOrigin {
    MACHINE,
    MANUAL,
}

enum class GlossaryMode {
    FIXED,
    KEEP,
}

object TranslationLocales {
    const val KOREAN: String = "ko"
    const val ENGLISH: String = "en"
    const val ANY: String = "*"
    private const val LOCALE_ECHO = 8
    val TARGETS: List<String> = listOf(ENGLISH, "ja", "zh")

    fun servingOrder(requested: String): List<String> =
        if (requested == KOREAN) emptyList() else listOf(requested, ENGLISH).distinct().filter { it in TARGETS }

    fun requireTarget(raw: String): String {
        val locale = raw.trim().lowercase(Locale.ROOT)
        require(locale in TARGETS) { "번역 대상 언어가 아닙니다: ${locale.take(LOCALE_ECHO)}" }
        return locale
    }
}
