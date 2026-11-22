package com.sleekydz86.idolglow.glowcommunity.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordSource
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityKeywordsResult
import com.sleekydz86.idolglow.glowcommunity.application.port.`in`.GlowCommunityKeywordUseCase
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityKeywordGeneratorPort
import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityPostPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

@Service
class GlowCommunityKeywordService(
    private val glowCommunityPostPort: GlowCommunityPostPort,
    private val glowCommunityKeywordGeneratorPort: GlowCommunityKeywordGeneratorPort,
) : GlowCommunityKeywordUseCase {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = AtomicReference<CachedKeywords?>(null)

    override fun findKeywords(): GlowCommunityKeywordsResult {
        val now = LocalDateTime.now(clock)
        cache.get()?.takeIf { it.validAt(now) }?.let { return it.result }
        val result = fallback(loadSources(), now)
        cache.set(CachedKeywords(result, now.plus(FALLBACK_TTL)))
        return result
    }

    @Scheduled(
        fixedDelayString = "\${app.glow-community.keywords.refresh-interval-ms:10800000}",
        initialDelayString = "\${app.glow-community.keywords.initial-delay-ms:60000}",
    )
    fun refresh(): GlowCommunityKeywordsResult {
        val now = LocalDateTime.now(clock)
        val sources = loadSources()
        val generated = if (sources.isEmpty()) null else generate(sources)
        val result =
            generated
                ?.let { GlowCommunityKeywordsResult(it, GlowCommunityKeywordsResult.SOURCE_LLM, now) }
                ?: fallback(sources, now)
        val ttl = if (generated != null) LLM_TTL else FALLBACK_TTL
        cache.set(CachedKeywords(result, now.plus(ttl)))
        return result
    }

    private fun generate(sources: List<GlowCommunityKeywordSource>): List<String>? {
        val keywords =
            runCatching { glowCommunityKeywordGeneratorPort.generate(sources) }
                .onFailure { log.warn("지역사회 추천 검색어 생성 실패 error={}", it.message) }
                .getOrNull()
                .orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(MAX_KEYWORDS)
        return keywords.takeIf { it.size >= MIN_KEYWORDS }
    }

    private fun fallback(
        sources: List<GlowCommunityKeywordSource>,
        now: LocalDateTime,
    ): GlowCommunityKeywordsResult =
        GlowCommunityKeywordsResult(
            keywords = GlowCommunityKeywordExtractor.extract(sources.map { it.title }, MAX_KEYWORDS),
            source = GlowCommunityKeywordsResult.SOURCE_FALLBACK,
            generatedAt = now,
        )

    private fun loadSources(): List<GlowCommunityKeywordSource> =
        (
            glowCommunityPostPort.findRecentVisible(RECENT_POST_LIMIT) +
                glowCommunityPostPort.findPopularVisible(POPULAR_POST_LIMIT)
        ).distinctBy { it.id }
            .map { GlowCommunityKeywordSource(title = it.title, snippet = snippetOf(it)) }

    private fun snippetOf(post: GlowCommunityPost): String =
        post.contentMarkdown
            .replace(URL, " ")
            .replace(MARKDOWN_SYMBOLS, " ")
            .replace(WHITESPACE, " ")
            .trim()
            .take(SNIPPET_LENGTH)

    private data class CachedKeywords(
        val result: GlowCommunityKeywordsResult,
        val expiresAt: LocalDateTime,
    ) {
        fun validAt(now: LocalDateTime): Boolean = now.isBefore(expiresAt)
    }

    companion object {
        const val MIN_KEYWORDS = 5
        const val MAX_KEYWORDS = 8
        private const val RECENT_POST_LIMIT = 30
        private const val POPULAR_POST_LIMIT = 20
        private const val SNIPPET_LENGTH = 80
        private val LLM_TTL: Duration = Duration.ofHours(6)
        private val FALLBACK_TTL: Duration = Duration.ofMinutes(10)
        private val URL = Regex("https?://\\S+")
        private val MARKDOWN_SYMBOLS = Regex("[#*>`\\[\\]()!_~|-]+")
        private val WHITESPACE = Regex("\\s+")
    }
}
