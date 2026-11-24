package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.GroundedChunkToEmbed
import com.sleekydz86.idolglow.groundedsearch.application.GroundedPrepareResult
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedChunkIndexPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChunkDraft
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedContentHash
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVectorCodec
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunkEmbedding
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroundedChunkIndexStore(
    private val chunkRepository: RagChunkJpaRepository,
    private val embeddingRepository: RagChunkEmbeddingJpaRepository,
) : GroundedChunkIndexPort {
    @Transactional
    override fun prepare(drafts: List<GroundedChunkDraft>, model: String): GroundedPrepareResult {
        val existing = chunkRepository.findAll().associateBy { it.sourceType to it.sourceId }.toMutableMap()
        val embeddings = embeddingRepository.findAll().associateBy { it.chunkId }
        val toEmbed = mutableListOf<GroundedChunkToEmbed>()
        var skipped = 0
        drafts.forEach { draft ->
            val row = collect(draft, existing, embeddings, model)
            if (row == null) {
                skipped += 1
            } else {
                toEmbed += row
            }
        }
        return GroundedPrepareResult(toEmbed, skipped)
    }

    @Transactional
    override fun saveEmbeddings(rows: List<GroundedChunkToEmbed>, vectors: List<FloatArray>, model: String) {
        require(rows.size == vectors.size) { "임베딩 개수가 문단 개수와 다릅니다." }
        rows.zip(vectors).forEach { (row, vector) -> saveOne(row, vector, model) }
    }

    private fun collect(
        draft: GroundedChunkDraft,
        existing: MutableMap<Pair<RagSourceType, Long>, RagChunk>,
        embeddings: Map<Long, RagChunkEmbedding>,
        model: String,
    ): GroundedChunkToEmbed? {
        val hash = GroundedContentHash.of(draft.content)
        val chunk = upsert(existing, draft, hash)
        if (sameEmbedding(embeddings[chunk.id], model, hash)) {
            return null
        }
        return GroundedChunkToEmbed(chunk.id, draft.content, hash)
    }

    private fun upsert(
        existing: MutableMap<Pair<RagSourceType, Long>, RagChunk>,
        draft: GroundedChunkDraft,
        hash: String,
    ): RagChunk {
        val found = existing[draft.sourceType to draft.sourceId]
        if (found == null) {
            val saved = chunkRepository.saveAndFlush(newChunk(draft, hash))
            existing[draft.sourceType to draft.sourceId] = saved
            return saved
        }
        found.linkKey = draft.linkKey
        found.content = draft.content
        found.contentHash = hash
        return found
    }

    private fun newChunk(draft: GroundedChunkDraft, hash: String): RagChunk =
        RagChunk(
            sourceType = draft.sourceType,
            sourceId = draft.sourceId,
            linkKey = draft.linkKey,
            content = draft.content,
            contentHash = hash,
        )

    private fun sameEmbedding(embedding: RagChunkEmbedding?, model: String, hash: String): Boolean {
        if (embedding == null) {
            return false
        }
        return embedding.model == model && embedding.contentHash == hash
    }

    private fun saveOne(row: GroundedChunkToEmbed, vector: FloatArray, model: String) {
        val found = embeddingRepository.findByChunkId(row.chunkId)
        if (found == null) {
            embeddingRepository.save(newEmbedding(row, vector, model))
            return
        }
        found.replace(model, vector, row.contentHash)
    }

    private fun newEmbedding(row: GroundedChunkToEmbed, vector: FloatArray, model: String): RagChunkEmbedding =
        RagChunkEmbedding(
            chunkId = row.chunkId,
            model = model,
            dims = vector.size,
            vectorBytes = GroundedVectorCodec.encode(vector),
            contentHash = row.contentHash,
        )
}
