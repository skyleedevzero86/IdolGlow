package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.translation.domain.GuardInput
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import com.sleekydz86.idolglow.translation.domain.TranslationGuard

object EventMachineTitle {
    private const val FIELD = "title"
    private val HANGUL = Regex("[\\uAC00-\\uD7A3]")
    private val GUARD_REASONS =
        listOf(
            "empty_field" to "EMPTY",
            "wrong_language" to "SCRIPT_MISSING",
            "kana_in_zh" to "WRONG_SCRIPT",
            "untranslated" to "HANGUL_LEFT",
            "hangul_left" to "HANGUL_LEFT",
            "number" to "NUMBER_CHANGED",
            "refusal" to "REFUSAL",
        )

    fun compose(
        locale: String,
        response: String,
        koreanTitle: String,
    ): String =
        if (EventLocale.normalize(locale) == EventLocale.ENGLISH) {
            "${response.trim()} (${koreanTitle.trim()})"
        } else {
            "${response.trim()}（${koreanTitle.trim()}）"
        }

    fun check(
        locale: String,
        koreanTitle: String,
        response: String,
        glossary: List<TranslationGlossaryTerm>,
    ): List<String> {
        val guard =
            TranslationGuard.check(
                GuardInput(
                    locale = EventLocale.normalize(locale),
                    source = mapOf(FIELD to koreanTitle),
                    translated = mapOf(FIELD to response),
                    tokenCounts = mapOf(FIELD to 0),
                    glossary = glossary,
                ),
            )
        val problems = (guard.hard + guard.soft).map(::toReason).toMutableList()
        if (HANGUL.containsMatchIn(response)) problems += "HANGUL_LEFT"
        val stored = compose(locale, response, koreanTitle)
        if (stored.codePointCount(0, stored.length) > EventTranslation.TITLE_MAX) problems += "TOO_LONG"
        return problems.distinct()
    }

    private fun toReason(guardReason: String): String =
        GUARD_REASONS.firstOrNull { (prefix, _) -> guardReason.startsWith(prefix) }?.second ?: "GUARD:$guardReason"
}
