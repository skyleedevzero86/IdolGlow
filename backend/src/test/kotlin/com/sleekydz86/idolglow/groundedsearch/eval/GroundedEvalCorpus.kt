package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedContentHash
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVectorCodec
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunkEmbedding
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkEmbeddingJpaRepository
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkJpaRepository
import org.springframework.jdbc.core.JdbcTemplate

object GroundedEvalCorpus {
    fun prepare(
        cases: List<GroundedEvalCase>,
        jdbcTemplate: JdbcTemplate,
        chunks: RagChunkJpaRepository,
        embeddings: RagChunkEmbeddingJpaRepository,
        embeddingPort: EmbeddingPort,
    ): Map<Long, RagChunk> {
        ensureFulltext(jdbcTemplate)
        jdbcTemplate.update("delete from rag_chunk_embeddings")
        jdbcTemplate.update("delete from rag_chunks")
        val saved = sources(cases).map { save(it, chunks) }
        store(saved, embeddingPort, embeddings)
        return saved.associateBy { it.id }
    }

    private fun sources(cases: List<GroundedEvalCase>): List<GroundedEvalSource> =
        cases.flatMap { it.sources }.distinctBy { GroundedGoldenParser.sourceKey(it.type, it.id) }

    private fun save(source: GroundedEvalSource, chunks: RagChunkJpaRepository): RagChunk =
        chunks.saveAndFlush(
            RagChunk(
                sourceType = source.type,
                sourceId = source.id,
                linkKey = "${source.type.name.lowercase()}/${source.id}",
                content = source.content,
                contentHash = GroundedContentHash.of(source.content),
            ),
        )

    private fun store(
        chunks: List<RagChunk>,
        embeddingPort: EmbeddingPort,
        embeddings: RagChunkEmbeddingJpaRepository,
    ) {
        val vectors = embeddingPort.embed(chunks.map { it.content })
        if (vectors.size != chunks.size) {
            throw IllegalStateException("임베딩 개수가 문단 개수와 다릅니다.")
        }
        chunks.zip(vectors).forEach { (chunk, vector) ->
            embeddings.save(row(chunk, vector, embeddingPort.modelName()))
        }
    }

    private fun row(chunk: RagChunk, vector: FloatArray, model: String): RagChunkEmbedding =
        RagChunkEmbedding(
            chunkId = chunk.id,
            model = model,
            dims = vector.size,
            vectorBytes = GroundedVectorCodec.encode(vector),
            contentHash = chunk.contentHash,
        )

    private fun ensureFulltext(jdbcTemplate: JdbcTemplate) {
        val count =
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'rag_chunks'
                  and index_name = 'ft_rag_chunks_content'
                """.trimIndent(),
                Long::class.java,
            ) ?: 0L
        if (count == 0L) {
            jdbcTemplate.execute(
                "alter table rag_chunks add fulltext index ft_rag_chunks_content (content) with parser ngram",
            )
        }
    }
}
