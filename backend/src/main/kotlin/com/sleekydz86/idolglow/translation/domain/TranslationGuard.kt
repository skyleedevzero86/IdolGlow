package com.sleekydz86.idolglow.translation.domain

data class GuardResult(
    val hard: List<String>,
    val soft: List<String>,
) {
    fun passed(): Boolean = hard.isEmpty() && soft.isEmpty()
}

data class GuardInput(
    val locale: String,
    val source: Map<String, String>,
    val translated: Map<String, String>,
    val tokenCounts: Map<String, Int>,
    val glossary: List<TranslationGlossaryTerm>,
)

object TranslationGuard {
    private const val MIN_RATIO_SOURCE = 20
    private const val UNTRANSLATED_RATIO = 0.3
    private val SIGNIFICANT_NUMBER = Regex("\\d[\\d,.:]*\\d|\\d")
    private val HANGUL = Regex("[\\uAC00-\\uD7A3]")
    private val KANA = Regex("[\\u3040-\\u30FF]")
    private val HAN = Regex("[\\u4E00-\\u9FFF]")
    private val LATIN = Regex("[A-Za-z]")
    private val LETTER = Regex("\\p{L}")
    private val TOKEN = Regex("⟦\\d+⟧")
    private val REFUSALS =
        listOf(
            "i'm sorry",
            "i am sorry",
            "i cannot",
            "i can't",
            "as an ai",
            "죄송",
            "번역할 수 없",
            "申し訳",
            "翻訳できません",
            "抱歉",
            "无法翻译",
        )
    private const val MIN_LETTERS = 2
    private const val SIGNIFICANT_DIGITS = 3
    private const val EN_MIN = 0.5
    private const val EN_MAX = 6.0
    private const val JA_MIN = 0.3
    private const val JA_MAX = 4.0
    private const val ZH_MIN = 0.2
    private const val ZH_MAX = 3.0
    private val RATIO_RANGE = mapOf("en" to EN_MIN..EN_MAX, "ja" to JA_MIN..JA_MAX, "zh" to ZH_MIN..ZH_MAX)

    fun check(input: GuardInput): GuardResult {
        val hard = mutableListOf<String>()
        val soft = mutableListOf<String>()
        input.source.forEach { (field, original) ->
            val translated = input.translated[field]
            if (translated == null) {
                hard += "missing_field:$field"
                return@forEach
            }
            if (translated.isBlank()) {
                hard += "empty_field:$field"
                return@forEach
            }
            checkField(input, field, original, translated, hard, soft)
        }
        input.translated.keys
            .filter { it !in input.source }
            .forEach { hard += "unknown_field:$it" }
        return GuardResult(hard.distinct(), soft.distinct())
    }

    private fun checkField(
        input: GuardInput,
        field: String,
        original: String,
        translated: String,
        hard: MutableList<String>,
        soft: MutableList<String>,
    ) {
        val lowered = translated.lowercase()
        if (REFUSALS.any { lowered.contains(it) } && REFUSALS.none { original.lowercase().contains(it) }) {
            hard += "refusal:$field"
        }
        TranslationProtector.tokenProblems(translated, input.tokenCounts[field] ?: 0).forEach { hard += "$it:$field" }
        missingNumbers(original, translated).forEach { hard += "number:$field:$it" }
        val glossary = input.glossary.filter { it.appliesTo(input.locale) && original.contains(it.sourceTerm) }
        glossary.filter { !translated.contains(it.expected()) }.forEach { soft += "glossary:$field:${it.sourceTerm}" }
        val cleaned = cleanForScript(translated, glossary)
        scriptProblems(input.locale, cleaned, hard, soft, field)
        lengthProblem(input.locale, original, translated)?.let { soft += "$it:$field" }
    }

    private fun missingNumbers(
        original: String,
        translated: String,
    ): List<String> {
        val have = numbers(TOKEN.replace(translated, " "))
        return numbers(TOKEN.replace(original, " "))
            .filter { it.length >= SIGNIFICANT_DIGITS || it.contains(',') || it.contains(':') || it.contains('.') }
            .filter { normalize(it) !in have.map(::normalize) }
            .distinct()
    }

    private fun numbers(text: String): List<String> = SIGNIFICANT_NUMBER.findAll(text).map { it.value }.toList()

    private fun normalize(number: String): String = number.replace(",", "")

    private fun cleanForScript(
        translated: String,
        glossary: List<TranslationGlossaryTerm>,
    ): String =
        glossary.fold(TOKEN.replace(translated, " ")) { text, term ->
            text.replace(term.expected(), " ").replace(term.sourceTerm, " ")
        }

    private fun scriptProblems(
        locale: String,
        text: String,
        hard: MutableList<String>,
        soft: MutableList<String>,
        field: String,
    ) {
        val letters = LETTER.findAll(text).count()
        if (letters < MIN_LETTERS) return
        val hangul = HANGUL.findAll(text).count()
        if (hangul.toDouble() / letters > UNTRANSLATED_RATIO) {
            hard += "untranslated:$field"
        } else if (hangul > 0) {
            soft += "hangul_left:$field"
        }
        val wrongLanguage =
            when (locale) {
                "en" -> !LATIN.containsMatchIn(text)
                "ja" -> !KANA.containsMatchIn(text) && !HAN.containsMatchIn(text)
                "zh" -> !HAN.containsMatchIn(text)
                else -> false
            }
        if (wrongLanguage) hard += "wrong_language:$field"
        if (locale == "zh" && KANA.containsMatchIn(text)) soft += "kana_in_zh:$field"
    }

    private fun lengthProblem(
        locale: String,
        original: String,
        translated: String,
    ): String? {
        val range = RATIO_RANGE[locale]?.takeIf { original.length >= MIN_RATIO_SOURCE } ?: return null
        val ratio = translated.length.toDouble() / original.length
        return if (ratio in range) null else "length_ratio"
    }
}
