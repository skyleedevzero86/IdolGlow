package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.domain.GlossaryMode
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import tools.jackson.databind.ObjectMapper

object TranslationPrompt {
    const val VERSION: String = "v1"
    const val FEATURE: String = "translation"
    const val JUDGE_FEATURE: String = "translation-judge"
    private const val MIN_SCORE = 1
    private const val MAX_SCORE = 5

    private val LANGUAGE =
        mapOf(
            "en" to "English",
            "ja" to "Japanese",
            "zh" to "Simplified Chinese",
        )

    fun language(locale: String): String = LANGUAGE[locale] ?: locale

    fun system(
        locale: String,
        glossary: List<TranslationGlossaryTerm>,
    ): String =
        buildString {
            append("You are a professional localization translator for a Korean K-culture travel service. ")
            append("Translate the Korean values into ${language(locale)}.\n")
            append("Rules:\n")
            append("1. Output only one JSON object with exactly the same keys as the input. No explanation.\n")
            append("2. Every token like ⟦1⟧ must appear exactly once and unchanged.\n")
            append("3. Keep every number as digits. ")
            append("Keep prices, dates, years, phone numbers and 24-hour times exactly.\n")
            append("4. Keep markdown formatting, line breaks and emoji.\n")
            append("5. Do not add, omit or summarize anything.\n")
            val relevant = glossary.filter { it.appliesTo(locale) }
            if (relevant.isNotEmpty()) {
                append("Glossary (must follow):\n")
                relevant.forEach { term ->
                    if (term.mode == GlossaryMode.KEEP) {
                        append("- \"${term.sourceTerm}\" -> keep as \"${term.sourceTerm}\"\n")
                    } else {
                        append("- \"${term.sourceTerm}\" -> \"${term.targetTerm}\"\n")
                    }
                }
            }
        }

    fun judgeSystem(locale: String): String =
        "You review translations from Korean into ${language(locale)}. " +
            "Score how faithfully the translation keeps the meaning of the source: " +
            "5 = same meaning, 4 = minor wording issue, 3 = something missing or added, 1-2 = wrong. " +
            "Output only JSON: {\"score\": <1-5>, \"issues\": [\"...\"]}."

    fun parseFields(
        raw: String,
        objectMapper: ObjectMapper,
    ): Map<String, String>? {
        val json = extractObject(raw) ?: return null
        return runCatching {
            objectMapper
                .readTree(json)
                .properties()
                .associate { (key, value) -> key to (if (value.isTextual) value.asText() else value.toString()) }
        }.getOrNull()
    }

    fun parseScore(
        raw: String,
        objectMapper: ObjectMapper,
    ): Int? {
        val json = extractObject(raw) ?: return null
        return runCatching { objectMapper.readTree(json).path("score").asInt(0) }
            .getOrNull()
            ?.takeIf { it in MIN_SCORE..MAX_SCORE }
    }

    private fun extractObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else null
    }
}
