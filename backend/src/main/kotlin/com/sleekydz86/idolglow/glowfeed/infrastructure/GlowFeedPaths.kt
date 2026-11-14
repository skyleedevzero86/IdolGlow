package com.sleekydz86.idolglow.glowfeed.infrastructure

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object GlowFeedPaths {
    private val DOT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val MARKDOWN_IMAGE = Regex("!\\[[^\\]]*]\\(\\s*([^)\\s]+)[^)]*\\)")
    private val SAFE_IMAGE = Regex("^(https?://|/)[^\\s\"'<>]*$")
    private val PUBLIC_COMMENT_PATHS = setOf("GLOW" to "/glow", "ARCHIVE" to "/archive").toMap()

    fun idolglowArticle(
        issueSlug: String,
        articleSlug: String,
    ): String = "/glow?tab=trend&issue=${encode(issueSlug)}&article=${encode(articleSlug)}"

    fun eventInfo(
        source: String,
        sourceId: String,
    ): String = "/event-info/${encode(source)}/${encode(sourceId)}"

    fun communityPost(postId: Long): String = "/glow?tab=local&post=$postId"

    fun product(productId: Long): String = "/products/$productId"

    fun commentTarget(
        targetType: String,
        targetKey: String,
        storedPath: String?,
    ): String? {
        storedPath?.trim()?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { return it }
        return when (targetType) {
            "PRODUCT" -> targetKey.toLongOrNull()?.let(::product)
            "GLOW_COMMUNITY_POST" -> targetKey.toLongOrNull()?.let(::communityPost)
            "ARTICLE" -> articleFromKey(targetKey)
            "EVENT_INFO" -> eventFromKey(targetKey)
            else -> PUBLIC_COMMENT_PATHS[targetType]
        }
    }

    fun dateRange(
        start: LocalDate?,
        end: LocalDate?,
    ): String? =
        when {
            start != null && end != null && start != end -> "${start.format(DOT_DATE)} ~ ${end.format(DOT_DATE)}"
            start != null -> start.format(DOT_DATE)
            end != null -> "~ ${end.format(DOT_DATE)}"
            else -> null
        }

    fun joinMeta(vararg parts: String?): String? =
        parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" · ").ifEmpty { null }

    fun firstMarkdownImage(markdown: String?): String? =
        markdown
            ?.let { MARKDOWN_IMAGE.find(it)?.groupValues?.get(1) }
            ?.takeIf { SAFE_IMAGE.matches(it) }

    private fun articleFromKey(key: String): String? {
        val issueSlug = key.substringBefore('/', "")
        val articleSlug = key.substringAfter('/', "")
        return if (issueSlug.isEmpty() || articleSlug.isEmpty()) null else idolglowArticle(issueSlug, articleSlug)
    }

    private fun eventFromKey(key: String): String? {
        val source = key.substringBefore(':', "")
        val sourceId = key.substringAfter(':', "")
        return if (source.isEmpty() || sourceId.isEmpty()) null else eventInfo(source, sourceId)
    }

    private fun encode(value: String): String =
        URLEncoder
            .encode(value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
}
