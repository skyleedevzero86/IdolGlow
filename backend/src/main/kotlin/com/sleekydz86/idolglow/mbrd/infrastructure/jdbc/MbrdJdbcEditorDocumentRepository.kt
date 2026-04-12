package com.sleekydz86.idolglow.mbrd.infrastructure.jdbc

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentId
import com.sleekydz86.idolglow.mbrd.domain.MbrdDocumentPublicationStatus
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocument
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorDocumentRepository
import com.sleekydz86.idolglow.mbrd.infrastructure.MbrdEditorEmbeddingEncoder
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

@Repository
class MbrdJdbcEditorDocumentRepository(
    private val dataSource: DataSource,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val embeddingEncoder: MbrdEditorEmbeddingEncoder,
) : MbrdEditorDocumentRepository {

    private val dbProduct: String = dataSource.connection.use { it.metaData.databaseProductName }

    private val isPostgres: Boolean =
        dbProduct.equals("PostgreSQL", ignoreCase = true)

    private val isMysql: Boolean =
        dbProduct.equals("MySQL", ignoreCase = true) ||
            dbProduct.contains("MariaDB", ignoreCase = true)

    @Volatile
    private var pgSearchAvailable: Boolean? = null

    override fun findById(id: MbrdDocumentId): MbrdEditorDocument? =
        jdbcTemplate.query(
            baseSelect() + " WHERE id = :id",
            mapOf("id" to id.value),
            rowMapper,
        ).firstOrNull()

    override fun findByUrlSlug(urlSlug: String): MbrdEditorDocument? {
        val normalized = normalizeQuery(urlSlug)
        if (normalized.isBlank()) return null
        return jdbcTemplate.query(
            baseSelect() +
                " WHERE url_slug = :urlSlug AND publication_status = :status ORDER BY updated_at DESC LIMIT 1",
            mapOf(
                "urlSlug" to normalized,
                "status" to MbrdDocumentPublicationStatus.PUBLISHED.name,
            ),
            rowMapper,
        ).firstOrNull()
    }

    override fun findLatest(): MbrdEditorDocument? =
        jdbcTemplate.query(
            baseSelect() + " ORDER BY updated_at DESC LIMIT 1",
            MapSqlParameterSource(),
            rowMapper,
        ).firstOrNull()

    override fun findLatestByStatus(status: MbrdDocumentPublicationStatus): MbrdEditorDocument? =
        jdbcTemplate.query(
            baseSelect() + " WHERE publication_status = :status ORDER BY updated_at DESC LIMIT 1",
            mapOf("status" to status.name),
            rowMapper,
        ).firstOrNull()

    override fun findPrevious(
        id: MbrdDocumentId,
        updatedAt: Instant,
        status: MbrdDocumentPublicationStatus,
    ): MbrdEditorDocument? =
        jdbcTemplate.query(
            baseSelect() +
                " WHERE id <> :id AND publication_status = :status AND updated_at > :updatedAt ORDER BY updated_at ASC LIMIT 1",
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("status", status.name)
                .addValue("updatedAt", Timestamp.from(updatedAt)),
            rowMapper,
        ).firstOrNull()

    override fun findNext(
        id: MbrdDocumentId,
        updatedAt: Instant,
        status: MbrdDocumentPublicationStatus,
    ): MbrdEditorDocument? =
        jdbcTemplate.query(
            baseSelect() +
                " WHERE id <> :id AND publication_status = :status AND updated_at < :updatedAt ORDER BY updated_at DESC LIMIT 1",
            MapSqlParameterSource()
                .addValue("id", id.value)
                .addValue("status", status.name)
                .addValue("updatedAt", Timestamp.from(updatedAt)),
            rowMapper,
        ).firstOrNull()

    override fun findPage(
        page: Int,
        size: Int,
        query: String,
        embeddingLiteral: String,
        status: MbrdDocumentPublicationStatus,
    ): List<MbrdEditorDocument> {
        val normalizedQuery = normalizeQuery(query)
        val searching = normalizedQuery.isNotBlank()
        val usePgSearch = searching && isPgSearchAvailable()
        val params = MapSqlParameterSource()
            .addValue("limit", size)
            .addValue("offset", page * size)
            .addValue("query", normalizedQuery)
            .addValue("likeQuery", "%$normalizedQuery%")
            .addValue("embedding", embeddingLiteral)
            .addValue("status", status.name)
        return jdbcTemplate.query(selectPageSql(searching, usePgSearch), params, rowMapper)
    }

    override fun count(query: String, status: MbrdDocumentPublicationStatus): Long {
        val normalizedQuery = normalizeQuery(query)
        val searching = normalizedQuery.isNotBlank()
        val usePgSearch = searching && isPgSearchAvailable()
        return jdbcTemplate.queryForObject(
            countSql(searching, usePgSearch),
            mapOf(
                "query" to normalizedQuery,
                "likeQuery" to "%$normalizedQuery%",
                "status" to status.name,
            ),
            Long::class.java,
        ) ?: 0L
    }

    override fun deleteById(id: MbrdDocumentId): Boolean {
        val rows = jdbcTemplate.update(
            "DELETE FROM editor_documents WHERE id = :id",
            mapOf("id" to id.value),
        )
        return rows > 0
    }

    override fun save(document: MbrdEditorDocument): MbrdEditorDocument {
        val params = documentParameters(document)
        val updated = jdbcTemplate.update(updateSql(), params)
        if (updated == 0) {
            jdbcTemplate.update(insertSql(), params)
        }
        return document
    }

    private fun updateSql(): String {
        val tagsExpr = tagsJsonUpdateExpression()
        return """
            UPDATE editor_documents
            SET title = :title,
                author = :author,
                markdown = :markdown,
                tags_json = $tagsExpr,
                url_slug = :urlSlug,
                introduction = :introduction,
                thumbnail_image_url = :thumbnailImageUrl,
                publication_status = :publicationStatus,
                updated_at = :updatedAt,
                embedding = :embedding
            WHERE id = :id
        """.trimIndent()
    }

    private fun insertSql(): String {
        val tagsExpr = tagsJsonInsertExpression()
        return """
            INSERT INTO editor_documents (
                id, title, author, markdown, tags_json, url_slug, introduction, thumbnail_image_url,
                publication_status, updated_at, embedding
            ) VALUES (
                :id, :title, :author, :markdown, $tagsExpr, :urlSlug, :introduction, :thumbnailImageUrl,
                :publicationStatus, :updatedAt, :embedding
            )
        """.trimIndent()
    }

    private fun tagsJsonUpdateExpression(): String =
        when {
            isPostgres -> "CAST(:tagsJson AS jsonb)"
            else -> ":tagsJson"
        }

    private fun tagsJsonInsertExpression(): String =
        when {
            isPostgres -> "CAST(:tagsJson AS jsonb)"
            else -> ":tagsJson"
        }

    private fun documentParameters(document: MbrdEditorDocument): MapSqlParameterSource {
        val tagsJson = objectMapper.writeValueAsString(document.tags)
        val embedding = embeddingEncoder.encode(document.title + "\n" + document.markdown)
        return MapSqlParameterSource()
            .addValue("id", document.id.value)
            .addValue("title", document.title)
            .addValue("author", document.author)
            .addValue("markdown", document.markdown)
            .addValue("tagsJson", tagsJson)
            .addValue("urlSlug", document.urlSlug)
            .addValue("introduction", document.introduction)
            .addValue("thumbnailImageUrl", document.thumbnailImageUrl)
            .addValue("publicationStatus", document.publicationStatus.name)
            .addValue("updatedAt", Timestamp.from(document.updatedAt))
            .addValue("embedding", embedding)
    }

    private fun selectPageSql(searching: Boolean, usePgSearch: Boolean): String {
        if (!searching) {
            return baseSelect() +
                " WHERE publication_status = :status ORDER BY updated_at DESC LIMIT :limit OFFSET :offset"
        }
        if (!usePgSearch) {
            return baseSelect() + " WHERE publication_status = :status AND " + textSearchPredicate() +
                " ORDER BY updated_at DESC LIMIT :limit OFFSET :offset"
        }
        return baseSelect() + " WHERE publication_status = :status AND " + pgSearchPredicate() +
            " ORDER BY updated_at DESC LIMIT :limit OFFSET :offset"
    }

    private fun countSql(searching: Boolean, usePgSearch: Boolean): String {
        if (!searching) {
            return "SELECT COUNT(*) FROM editor_documents WHERE publication_status = :status"
        }
        if (!usePgSearch) {
            return "SELECT COUNT(*) FROM editor_documents WHERE publication_status = :status AND " + textSearchPredicate()
        }
        return "SELECT COUNT(*) FROM editor_documents WHERE publication_status = :status AND " + pgSearchPredicate()
    }

    private fun textSearchPredicate(): String =
        if (isMysql) {
            "(LOWER(title) LIKE LOWER(:likeQuery) OR LOWER(author) LIKE LOWER(:likeQuery) OR LOWER(markdown) LIKE LOWER(:likeQuery))"
        } else {
            "(title ILIKE :likeQuery OR author ILIKE :likeQuery OR markdown ILIKE :likeQuery)"
        }

    private fun pgSearchPredicate(): String =
        "(title @@@ :query OR author @@@ :query OR markdown @@@ :query)"

    private fun baseSelect(): String =
        """
        SELECT id, title, author, markdown, tags_json, url_slug, introduction, thumbnail_image_url, publication_status, updated_at
        FROM editor_documents
        """.trimIndent()

    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        mapDocumentRow(rs)
    }

    private fun mapDocumentRow(rs: ResultSet): MbrdEditorDocument {
        val updatedAt = rs.getTimestamp("updated_at")?.toInstant() ?: Instant.now()
        val rawStatus = rs.getString("publication_status")
        val publicationStatus = parsePublicationStatus(rawStatus)
        val idObj = rs.getObject("id")
        val uuid = when (idObj) {
            is UUID -> idObj
            is String -> UUID.fromString(idObj)
            else -> UUID.fromString(idObj.toString())
        }
        return MbrdEditorDocument(
            id = MbrdDocumentId(uuid),
            title = rs.getString("title"),
            author = rs.getString("author"),
            markdown = rs.getString("markdown"),
            tags = readTags(rs.getString("tags_json")),
            urlSlug = rs.getString("url_slug"),
            introduction = rs.getString("introduction"),
            thumbnailImageUrl = rs.getString("thumbnail_image_url"),
            publicationStatus = publicationStatus,
            updatedAt = updatedAt,
        )
    }

    private fun readTags(rawJson: String?): List<String> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(rawJson, object : TypeReference<List<String>>() {})
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePublicationStatus(raw: String?): MbrdDocumentPublicationStatus {
        if (raw.isNullOrBlank()) return MbrdDocumentPublicationStatus.PUBLISHED
        return try {
            MbrdDocumentPublicationStatus.valueOf(raw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            MbrdDocumentPublicationStatus.PUBLISHED
        }
    }

    private fun normalizeQuery(query: String?): String = query?.trim() ?: ""

    private fun isPgSearchAvailable(): Boolean {
        if (!isPostgres) return false
        pgSearchAvailable?.let { return it }
        synchronized(this) {
            pgSearchAvailable?.let { return it }
            val available = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_search')",
                MapSqlParameterSource(),
                Boolean::class.java,
            ) ?: false
            pgSearchAvailable = available
            return available
        }
    }
}
