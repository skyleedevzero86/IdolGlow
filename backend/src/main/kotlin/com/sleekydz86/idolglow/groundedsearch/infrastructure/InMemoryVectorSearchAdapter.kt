package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVectorCodec
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InMemoryVectorSearchAdapter(
    private val embeddingRepository: RagChunkEmbeddingJpaRepository,
) : VectorSearchPort {
    @Transactional(readOnly = true)
    override fun search(vector: FloatArray, limit: Int): List<Long> =
        embeddingRepository
            .findAll()
            .map { embedding ->
                val decoded = GroundedVectorCodec.decode(embedding.vectorBytes, embedding.dims)
                embedding.chunkId to GroundedVectorCodec.cosine(vector, decoded)
            }.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
}
