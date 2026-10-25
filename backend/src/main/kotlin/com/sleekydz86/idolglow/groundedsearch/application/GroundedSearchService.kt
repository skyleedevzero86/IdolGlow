package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedHitPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedRetrievePort
import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedRetrieval
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedScope
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchLimits
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import com.sleekydz86.idolglow.groundedsearch.domain.ReciprocalRankFusion
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class GroundedSearchService(
    private val keywordSearchPort: KeywordSearchPort,
    private val vectorSearchPort: VectorSearchPort,
    private val embeddingPort: EmbeddingPort,
    private val rerankPort: RerankPort,
    private val hitPort: GroundedHitPort,
) : GroundedRetrievePort {
    fun search(query: String): List<GroundedSearchHit> = retrieve(query, GroundedScope.unfiltered()).hits

    override fun retrieve(query: String, scope: GroundedScope): GroundedRetrieval {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "검색어는 비어 있을 수 없습니다." }
        val keywordIds = keywordSearchPort.search(trimmed, GroundedSearchLimits.keywordLimit())
        val vectorIds = vectorSearchPort.search(queryVector(trimmed), GroundedSearchLimits.vectorLimit())
        val fused =
            ReciprocalRankFusion.merge(
                keywordIds,
                vectorIds,
                GroundedSearchLimits.rrfK(),
                GroundedSearchLimits.resultLimit(),
            )
        val hits = rerankPort.rerank(trimmed, hitPort.read(fused).filter { keep(it, scope) })
        return GroundedRetrieval(hits, keywordIds, vectorIds, fused)
    }

    private fun keep(hit: GroundedSearchHit, scope: GroundedScope): Boolean {
        val ids = scope.productIds ?: return true
        return owner(hit, scope) in ids
    }

    private fun owner(hit: GroundedSearchHit, scope: GroundedScope): Long? =
        when (hit.sourceType) {
            RagSourceType.PRODUCT -> hit.sourceId
            RagSourceType.OPTION -> scope.optionToProduct[hit.sourceId]
            else -> null
        }

    private fun queryVector(query: String): FloatArray {
        val vector = embeddingPort.embed(listOf(query)).firstOrNull()
        if (vector == null) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "검색어 임베딩 결과가 비어 있습니다.")
        }
        return vector
    }
}
