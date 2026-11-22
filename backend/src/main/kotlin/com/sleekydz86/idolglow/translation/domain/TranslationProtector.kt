package com.sleekydz86.idolglow.translation.domain

data class ProtectedText(
    val text: String,
    val tokens: List<String>,
)

object TranslationProtector {
    private val PROTECTED =
        listOf(
            Regex("```[\\s\\S]*?```"),
            Regex("!\\[[^\\]]*]\\([^)]*\\)"),
            Regex("(?<=])\\([^)]*\\)"),
            Regex("`[^`\\n]+`"),
            Regex("<[^<>\\n]+>"),
            Regex("https?://[^\\s)\\]]+"),
            Regex("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"),
        )
    private val TOKEN = Regex("⟦(\\d+)⟧")

    fun token(index: Int): String = "⟦$index⟧"

    fun protect(text: String): ProtectedText {
        val tokens = mutableListOf<String>()
        val protectedText =
            PROTECTED.fold(text) { current, pattern ->
                pattern.replace(current) { match ->
                    tokens += match.value
                    token(tokens.size)
                }
            }
        return ProtectedText(protectedText, tokens)
    }

    fun tokenProblems(
        translated: String,
        tokenCount: Int,
    ): List<String> {
        val found = TOKEN.findAll(translated).map { it.groupValues[1].toInt() }.toList()
        val expected = (1..tokenCount).toList()
        return buildList {
            expected.filter { index -> found.count { it == index } != 1 }.forEach { add("token_${it}_count") }
            found.filter { it !in expected }.distinct().forEach { add("token_${it}_unknown") }
        }
    }

    fun restore(
        translated: String,
        tokens: List<String>,
    ): String =
        TOKEN.replace(translated) { match ->
            val index = match.groupValues[1].toInt()
            tokens.getOrNull(index - 1) ?: match.value
        }
}
