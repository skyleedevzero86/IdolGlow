package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedHitPort
import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.FusedRank
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionCheck
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchLimits
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.ReciprocalRankFusion

class GroundedEvalSession(
    private val keywordSearchPort: KeywordSearchPort,
    private val vectorSearchPort: VectorSearchPort,
    private val embeddingPort: EmbeddingPort,
    private val hitPort: GroundedHitPort,
    private val rerankPort: RerankPort,
    private val extractPort: ConditionExtractPort,
) {
    fun modes(
        cases: List<GroundedEvalCase>,
        chunks: Map<Long, RagChunk>,
        rerank: Boolean,
    ): List<GroundedEvalModeScore> {
        val scores =
            mutableListOf(
                aggregate(GroundedEvalReport.KEYWORD, cases) { keyword(it.question, chunks) },
                aggregate(GroundedEvalReport.VECTOR, cases) { vector(it.question, chunks) },
                aggregate(GroundedEvalReport.HYBRID, cases) { hybrid(it.question, chunks) },
            )
        if (rerank) {
            scores += aggregate(GroundedEvalReport.RERANK, cases) { reranked(it.question) }
        }
        return scores
    }

    fun fieldRates(cases: List<GroundedEvalCase>): Map<String, Double> {
        val rows = cases.map { GroundedEvalScore.same(checked(it.question), it.expected) }
        return rows.first().keys.associateWith { field ->
            GroundedEvalScore.mean(rows.map { row -> if (row.getValue(field)) 1.toDouble() else 0.toDouble() })
        }
    }

    private fun aggregate(
        mode: String,
        cases: List<GroundedEvalCase>,
        rank: (GroundedEvalCase) -> List<String>,
    ): GroundedEvalModeScore {
        val ranked =
            cases.map { item ->
                val keys = item.sources.map { source -> GroundedGoldenParser.sourceKey(source.type, source.id) }
                rank(item) to keys.toSet()
            }
        return GroundedEvalModeScore(
            mode = mode,
            recall =
                GroundedEvalScore.mean(
                    ranked.map { GroundedEvalScore.recallAt(it.first, it.second, GroundedEvalScore.cutoff()) },
                ),
            mrr = GroundedEvalScore.mean(ranked.map { GroundedEvalScore.reciprocalRank(it.first, it.second) }),
        )
    }

    private fun keyword(question: String, chunks: Map<Long, RagChunk>): List<String> =
        keys(keywordSearchPort.search(question, GroundedSearchLimits.keywordLimit()), chunks)

    private fun vector(question: String, chunks: Map<Long, RagChunk>): List<String> =
        keys(vectorSearchPort.search(queryVector(question), GroundedSearchLimits.vectorLimit()), chunks)

    private fun hybrid(question: String, chunks: Map<Long, RagChunk>): List<String> =
        keys(fuse(question).map { it.chunkId }, chunks)

    private fun reranked(question: String): List<String> =
        rerankPort.rerank(question, hitPort.read(fuse(question))).map {
            GroundedGoldenParser.sourceKey(it.sourceType, it.sourceId)
        }

    private fun fuse(question: String): List<FusedRank> {
        val keywordIds = keywordSearchPort.search(question, GroundedSearchLimits.keywordLimit())
        val vectorIds = vectorSearchPort.search(queryVector(question), GroundedSearchLimits.vectorLimit())
        return ReciprocalRankFusion.merge(
            keywordIds,
            vectorIds,
            GroundedSearchLimits.rrfK(),
            GroundedSearchLimits.resultLimit(),
        )
    }

    private fun keys(ids: List<Long>, chunks: Map<Long, RagChunk>): List<String> =
        ids.mapNotNull { id ->
            chunks[id]?.let { GroundedGoldenParser.sourceKey(it.sourceType, it.sourceId) }
        }

    private fun queryVector(question: String): FloatArray {
        val vector = embeddingPort.embed(listOf(question)).firstOrNull()
        if (vector == null) {
            throw IllegalStateException("검색어 임베딩 결과가 비어 있습니다.")
        }
        return vector
    }

    private fun checked(question: String): GroundedChecked =
        GroundedConditionCheck.accept(extractPort.extract(question))
}
