package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordSource
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityKeywordGeneratorPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class GlowCommunityKeywordLlmAdapter(
    private val llmGateway: LlmGateway,
    private val llmInputMasker: LlmInputMasker,
    private val objectMapper: ObjectMapper,
) : GlowCommunityKeywordGeneratorPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun generate(sources: List<GlowCommunityKeywordSource>): List<String>? {
        val result =
            sources.takeIf { it.isNotEmpty() }?.let {
                llmGateway.complete(
                    LlmRequest(
                        feature = FEATURE,
                        systemPrompt = SYSTEM_PROMPT,
                        userPrompt = llmInputMasker.mask(userPrompt(it)),
                        maxOutputTokens = MAX_OUTPUT_TOKENS,
                        temperature = TEMPERATURE,
                    ),
                )
            } ?: return null
        val keywords = parse(result.text).takeIf { it.size >= MIN_KEYWORDS }
        if (keywords == null) log.warn("지역사회 추천 검색어 응답 버림 provider={}", result.provider)
        return keywords
    }

    fun parse(text: String): List<String> {
        val json =
            text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        val root = runCatching { objectMapper.readValue(json, Any::class.java) }.getOrNull()
        val values =
            when (root) {
                is Map<*, *> -> root["keywords"] as? List<*>
                is List<*> -> root
                else -> null
            }.orEmpty()
        return values
            .filterIsInstance<String>()
            .map { it.trim().trimStart('#').trim() }
            .filter(::isValidKeyword)
            .distinctBy { it.lowercase() }
            .take(MAX_KEYWORDS)
    }

    private fun isValidKeyword(keyword: String): Boolean =
        keyword.length in MIN_KEYWORD_LENGTH..MAX_KEYWORD_LENGTH &&
            HANGUL.containsMatchIn(keyword) &&
            !FORBIDDEN.containsMatchIn(keyword)

    private fun userPrompt(sources: List<GlowCommunityKeywordSource>): String =
        sources
            .take(MAX_SOURCES)
            .mapIndexed { index, source -> "${index + 1}. 제목: ${source.title} / 요약: ${source.snippet}" }
            .joinToString(separator = "\n", prefix = "최근 인기 게시글\n")

    companion object {
        private const val FEATURE = "glow-community"
        private const val MAX_OUTPUT_TOKENS = 300
        private const val TEMPERATURE = 0.3
        private const val MAX_SOURCES = 40
        private const val MIN_KEYWORDS = 5
        private const val MAX_KEYWORDS = 8
        private const val MIN_KEYWORD_LENGTH = 2
        private const val MAX_KEYWORD_LENGTH = 20
        private val HANGUL = Regex("[가-힣]")
        private val FORBIDDEN = Regex("https?://|www\\.|@|[\\r\\n<>{}\\[\\]]")
        private val SYSTEM_PROMPT =
            """
            너는 한국 여행 커뮤니티의 검색어 추천기다.
            주어진 게시글 제목과 요약만 보고 방문자가 검색할 만한 짧은 한국어 검색어를 5개에서 8개 만든다.
            검색어는 2자에서 20자 사이의 명사구로 쓰고 장소, 음식, 교통, 체험 위주로 고른다.
            사람 이름, 닉네임, 이메일, 전화번호, 링크는 넣지 않는다.
            반드시 {"keywords": ["검색어1", "검색어2"]} 형식의 JSON 만 출력한다.
            """.trimIndent()
    }
}
