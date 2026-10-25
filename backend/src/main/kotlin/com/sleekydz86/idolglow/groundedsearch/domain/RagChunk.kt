package com.sleekydz86.idolglow.groundedsearch.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "rag_chunks",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_rag_chunk_source", columnNames = ["source_type", "source_id"]),
    ],
)
class RagChunk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    val sourceType: RagSourceType,
    @Column(name = "source_id", nullable = false)
    val sourceId: Long,
    @Column(name = "link_key", nullable = false, length = 400)
    var linkKey: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(name = "content_hash", nullable = false, length = 64)
    var contentHash: String,
) : BaseEntity()
