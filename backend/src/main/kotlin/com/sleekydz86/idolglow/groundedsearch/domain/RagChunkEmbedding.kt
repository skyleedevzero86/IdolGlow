package com.sleekydz86.idolglow.groundedsearch.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "rag_chunk_embeddings",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_rag_chunk_embedding_chunk", columnNames = ["chunk_id"]),
    ],
)
class RagChunkEmbedding(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "chunk_id", nullable = false)
    val chunkId: Long,
    @Column(nullable = false, length = 80)
    var model: String,
    @Column(nullable = false)
    var dims: Int,
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "vector_bytes", nullable = false)
    var vectorBytes: ByteArray,
    @Column(name = "content_hash", nullable = false, length = 64)
    var contentHash: String,
) {
    fun replace(model: String, vector: FloatArray, contentHash: String) {
        this.model = model
        this.dims = vector.size
        this.vectorBytes = GroundedVectorCodec.encode(vector)
        this.contentHash = contentHash
    }
}
