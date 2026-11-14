package com.sleekydz86.idolglow.glowcommunity.application

object GlowCommunityKeywordExtractor {
    private const val MIN_TOKEN_LENGTH = 2
    private const val MAX_TOKEN_LENGTH = 15
    private val SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
    private val DIGITS = Regex("\\d+")
    private val HANGUL_END = Regex(".*[가-힣]$")
    private val PARTICLES =
        listOf(
            "에서는", "으로", "에서", "에게", "까지", "부터", "처럼", "이랑",
            "은", "는", "이", "가", "을", "를", "에", "의", "도", "로", "와", "과", "랑",
        )
    private val PREDICATE_ENDINGS = listOf("요", "까", "죠", "할", "한", "된", "던", "는", "은")
    private val STOPWORDS =
        setOf(
            "있나요", "어디서", "어디", "어떻게", "무엇", "정말", "너무", "혹시", "그리고", "이번", "가장", "제일",
            "많이", "방법", "공유", "후기", "질문", "궁금", "추천", "추천해", "부탁", "같이", "함께", "오늘", "내일", "이유",
            "정리", "사람", "사람들", "합니다", "입니다", "있습니다", "했습니다", "the", "and", "for", "with",
        )

    fun extract(
        titles: List<String>,
        limit: Int,
    ): List<String> {
        val counts = LinkedHashMap<String, KeywordCount>()
        titles.forEach { title ->
            tokens(title)
                .distinctBy { it.lowercase() }
                .forEach { token -> counts.getOrPut(token.lowercase()) { KeywordCount(token) }.count += 1 }
        }
        return counts.values
            .sortedByDescending { it.count }
            .take(limit)
            .map { it.display }
    }

    fun tokens(text: String): List<String> =
        SEPARATOR
            .split(text)
            .map(::stripParticle)
            .filter(::isMeaningful)

    private fun stripParticle(token: String): String {
        if (!HANGUL_END.matches(token)) return token
        val particle = PARTICLES.firstOrNull { token.endsWith(it) && token.length - it.length >= MIN_TOKEN_LENGTH }
        return particle?.let { token.removeSuffix(it) } ?: token
    }

    private fun isMeaningful(token: String): Boolean =
        token.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH &&
            !DIGITS.matches(token) &&
            token.lowercase() !in STOPWORDS &&
            PREDICATE_ENDINGS.none { token.endsWith(it) }

    private class KeywordCount(
        val display: String,
        var count: Int = 0,
    )
}
