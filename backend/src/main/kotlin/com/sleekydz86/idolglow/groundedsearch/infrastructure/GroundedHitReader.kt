package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedHitPort
import com.sleekydz86.idolglow.groundedsearch.domain.FusedRank
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroundedHitReader(
    private val chunkRepository: RagChunkJpaRepository,
) : GroundedHitPort {
    @Transactional(readOnly = true)
    override fun read(fused: List<FusedRank>): List<GroundedSearchHit> {
        if (fused.isEmpty()) {
            return emptyList()
        }
        val chunks = chunkRepository.findAllById(fused.map { it.chunkId }).associateBy { it.id }
        return fused.mapNotNull { rank -> hit(chunks[rank.chunkId], rank) }
    }

    private fun hit(chunk: RagChunk?, rank: FusedRank): GroundedSearchHit? {
        if (chunk == null) {
            return null
        }
        return GroundedSearchHit(
            chunkId = chunk.id,
            sourceType = chunk.sourceType,
            sourceId = chunk.sourceId,
            linkKey = chunk.linkKey,
            content = chunk.content,
            keywordRank = rank.keywordRank,
            vectorRank = rank.vectorRank,
            rrfScore = rank.score,
        )
    }
}
