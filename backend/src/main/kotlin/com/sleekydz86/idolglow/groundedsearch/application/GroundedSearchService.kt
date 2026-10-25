package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedHitPort
import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchLimits
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
) {
    fun search(query: String): List<GroundedSearchHit> {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "검색어는 비어 있을 수 없습니다." }
        val keywordIds = keywordSearchPort.search(trimmed, GroundedSearchLimits.keywordLimit())
        val queryVector = queryVector(trimmed)
        val vectorIds = vectorSearchPort.search(queryVector, GroundedSearchLimits.vectorLimit())
        val fused =
            ReciprocalRankFusion.merge(
                keywordIds,
                vectorIds,
                GroundedSearchLimits.rrfK(),
                GroundedSearchLimits.resultLimit(),
            )
        return rerankPort.rerank(hitPort.read(fused))
    }

    private fun queryVector(query: String): FloatArray {
        val vector = embeddingPort.embed(listOf(query)).firstOrNull()
        if (vector == null) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "검색어 임베딩 결과가 비어 있습니다.")
        }
        return vector
    }
}
