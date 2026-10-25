package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.domain.RagChunkEmbedding
import org.springframework.data.jpa.repository.JpaRepository

interface RagChunkEmbeddingJpaRepository : JpaRepository<RagChunkEmbedding, Long> {
    fun findByChunkId(chunkId: Long): RagChunkEmbedding?
}
