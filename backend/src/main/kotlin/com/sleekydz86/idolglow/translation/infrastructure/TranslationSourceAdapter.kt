package com.sleekydz86.idolglow.translation.infrastructure

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentId
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentPublicationStatus
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocumentRepository
import com.sleekydz86.idolglow.translation.application.port.out.TranslationSourcePort
import com.sleekydz86.idolglow.translation.domain.SourceContent
import com.sleekydz86.idolglow.translation.domain.SourcePage
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Component
class TranslationSourceAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val documentRepository: MbrdEditorDocumentRepository,
    private val objectMapper: ObjectMapper,
) : TranslationSourcePort {
    override fun load(
        type: TranslationContentType,
        key: String,
    ): SourceContent? =
        when (type) {
            TranslationContentType.NOTICE, TranslationContentType.EVENT -> document(type, key)
            TranslationContentType.ARTICLE -> article(key)
            else -> {
                val id = key.toLongOrNull() ?: return null
                val params = MapSqlParameterSource("id", id).addValue("today", today())
                jdbcTemplate.query(singleSql(type), params) { rs, _ -> map(type, rs) }.firstOrNull()
            }
        }

    private fun article(key: String): SourceContent? {
        val issueSlug = key.substringBefore('/', "")
        val articleSlug = key.substringAfter('/', "")
        if (issueSlug.isEmpty() || articleSlug.isEmpty()) return null
        val params = MapSqlParameterSource("issueSlug", issueSlug).addValue("articleSlug", articleSlug)
        return jdbcTemplate
            .query(ARTICLE_SELECT + ARTICLE_BY_SLUG, params) { rs, _ -> map(TranslationContentType.ARTICLE, rs) }
            .firstOrNull()
    }

    override fun page(
        type: TranslationContentType,
        offset: Int,
        limit: Int,
    ): SourcePage {
        val params =
            MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset)
                .addValue("today", today())
                .addValue("published", MbrdDocumentPublicationStatus.PUBLISHED.name)
        return when (type) {
            TranslationContentType.NOTICE, TranslationContentType.EVENT -> {
                val rows = jdbcTemplate.query(DOCUMENT_PAGE_SQL, params) { rs, _ -> documentRow(type, rs) }
                SourcePage(rows.filterNotNull(), rows.size)
            }
            TranslationContentType.PRODUCT, TranslationContentType.ARTICLE -> {
                val rows = jdbcTemplate.query(pageSql(type), params) { rs, _ -> map(type, rs) }
                SourcePage(rows, rows.size)
            }
            else -> SourcePage(emptyList(), 0)
        }
    }

    private fun document(
        type: TranslationContentType,
        key: String,
    ): SourceContent? {
        val id = runCatching { UUID.fromString(key.trim()) }.getOrNull()
        val document = id?.let { documentRepository.findById(MbrdDocumentId(it)) } ?: return null
        val visible =
            document.publicationStatus == MbrdDocumentPublicationStatus.PUBLISHED &&
                hasMarker(type, document.tags)
        return SourceContent.of(
            type,
            document.id.value.toString(),
            mapOf("title" to document.title, "introduction" to document.introduction, "body" to document.markdown),
            visible,
        )
    }

    private fun documentRow(
        type: TranslationContentType,
        rs: ResultSet,
    ): SourceContent? {
        val tags = readTags(rs.getString("tags_json"))
        if (!hasMarker(type, tags)) return null
        return SourceContent.of(
            type,
            rs.getObject("id").toString(),
            mapOf(
                "title" to rs.getString("title"),
                "introduction" to rs.getString("introduction"),
                "body" to rs.getString("markdown"),
            ),
            true,
        )
    }

    private fun map(
        type: TranslationContentType,
        rs: ResultSet,
    ): SourceContent {
        val key =
            if (type == TranslationContentType.ARTICLE) {
                "${rs.getString("issue_slug")}/${rs.getString("article_slug")}"
            } else {
                rs.getLong("id").toString()
            }
        return when (type) {
            TranslationContentType.PRODUCT ->
                SourceContent.of(
                    type,
                    key,
                    mapOf("name" to rs.getString("name"), "description" to rs.getString("description")),
                    true,
                )
            TranslationContentType.ARTICLE -> {
                val issueDate = rs.getObject("issue_date", LocalDate::class.java)
                SourceContent.of(
                    type,
                    key,
                    mapOf(
                        "title" to rs.getString("title"),
                        "kicker" to rs.getString("kicker"),
                        "summary" to rs.getString("summary"),
                    ),
                    issueDate != null && !issueDate.isAfter(today()),
                )
            }
            TranslationContentType.GLOW_COMMUNITY_POST ->
                SourceContent.of(
                    type,
                    key,
                    mapOf("title" to rs.getString("title"), "content" to rs.getString("content_markdown")),
                    !rs.getBoolean("blinded") && rs.getTimestamp("deleted_at") == null,
                )
            TranslationContentType.CONTENT_COMMENT ->
                SourceContent.of(
                    type,
                    key,
                    mapOf("content" to rs.getString("content_markdown")),
                    rs.getTimestamp("deleted_at") == null,
                )
            TranslationContentType.NOTICE, TranslationContentType.EVENT ->
                throw IllegalStateException("문서 원문은 문서 저장소에서 읽어야 합니다. type=$type")
        }
    }

    private fun singleSql(type: TranslationContentType): String =
        when (type) {
            TranslationContentType.PRODUCT -> "SELECT id, name, description FROM products WHERE id = :id"
            TranslationContentType.ARTICLE -> ARTICLE_SELECT + ARTICLE_BY_SLUG
            TranslationContentType.GLOW_COMMUNITY_POST ->
                "SELECT id, title, content_markdown, blinded, deleted_at FROM glow_community_posts WHERE id = :id"
            TranslationContentType.CONTENT_COMMENT ->
                "SELECT id, content_markdown, deleted_at FROM content_comments WHERE id = :id"
            TranslationContentType.NOTICE, TranslationContentType.EVENT ->
                throw IllegalStateException("문서 원문은 문서 저장소에서 읽어야 합니다. type=$type")
        }

    private fun pageSql(type: TranslationContentType): String =
        when (type) {
            TranslationContentType.PRODUCT ->
                "SELECT id, name, description FROM products ORDER BY id LIMIT :limit OFFSET :offset"
            TranslationContentType.ARTICLE ->
                ARTICLE_SELECT + " WHERE i.issue_date <= :today ORDER BY a.id LIMIT :limit OFFSET :offset"
            else -> throw IllegalStateException("미리 번역하지 않는 콘텐츠입니다. type=$type")
        }

    private fun hasMarker(
        type: TranslationContentType,
        tags: List<String>,
    ): Boolean {
        val notice = tags.any { it.trim().equals(NOTICE_MARKER, ignoreCase = true) }
        return if (type == TranslationContentType.NOTICE) notice else !notice
    }

    private fun readTags(raw: String?): List<String> =
        raw?.let { runCatching { objectMapper.readValue(it, TAGS_TYPE) }.getOrNull() }.orEmpty()

    private fun today(): LocalDate = LocalDate.now(IdolGlowDateTimes.ZONE)

    companion object {
        private const val NOTICE_MARKER = "notice-board"
        private val TAGS_TYPE = object : TypeReference<List<String>>() {}
        private const val ARTICLE_SELECT =
            "SELECT a.id, i.slug AS issue_slug, a.slug AS article_slug, a.title, a.kicker, a.summary, i.issue_date " +
                "FROM idolglow_articles a " +
                "JOIN idolglow_issues i ON i.id = a.issue_id"
        private const val ARTICLE_BY_SLUG = " WHERE i.slug = :issueSlug AND a.slug = :articleSlug"
        private const val DOCUMENT_PAGE_SQL =
            "SELECT id, title, introduction, markdown, tags_json FROM editor_documents " +
                "WHERE publication_status = :published ORDER BY updated_at DESC, id LIMIT :limit OFFSET :offset"
    }
}
