package com.sleekydz86.idolglow.mbrd.infrastructure.jdbc

import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorAsset
import com.sleekydz86.idolglow.mbrd.domain.MbrdEditorAssetRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.UUID

@Repository
class MbrdJdbcEditorAssetRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : MbrdEditorAssetRepository {

    override fun save(asset: MbrdEditorAsset): MbrdEditorAsset {
        jdbcTemplate.update(
            """
            INSERT INTO editor_assets (id, object_key, bucket_name, original_file_name, content_type, file_size, uploaded_at)
            VALUES (:id, :objectKey, :bucketName, :originalFileName, :contentType, :fileSize, :uploadedAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", asset.id)
                .addValue("objectKey", asset.objectKey)
                .addValue("bucketName", asset.bucketName)
                .addValue("originalFileName", asset.originalFileName)
                .addValue("contentType", asset.contentType)
                .addValue("fileSize", asset.fileSize)
                .addValue("uploadedAt", Timestamp.from(asset.uploadedAt)),
        )
        return asset
    }

    override fun findById(id: UUID): MbrdEditorAsset? {
        val list = jdbcTemplate.query(
            """
            SELECT id, object_key, bucket_name, original_file_name, content_type, file_size, uploaded_at
            FROM editor_assets
            WHERE id = :id
            """.trimIndent(),
            mapOf("id" to id),
        ) { rs, _ ->
            MbrdEditorAsset(
                id = rs.getObject("id", UUID::class.java),
                objectKey = rs.getString("object_key"),
                bucketName = rs.getString("bucket_name"),
                originalFileName = rs.getString("original_file_name"),
                contentType = rs.getString("content_type"),
                fileSize = rs.getLong("file_size"),
                uploadedAt = rs.getTimestamp("uploaded_at").toInstant(),
            )
        }
        return list.firstOrNull()
    }
}
