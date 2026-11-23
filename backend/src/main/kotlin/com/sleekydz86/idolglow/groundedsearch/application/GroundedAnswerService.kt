package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.AnswerGeneratePort
import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedCatalogPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedRetrievePort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswer
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswerCheck
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionCheck
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedRetrieval
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedScope
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVerdict
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GroundedAnswerService(
    private val extractPort: ConditionExtractPort,
    private val answerPort: AnswerGeneratePort,
    private val catalogPort: GroundedCatalogPort,
    private val retrievePort: GroundedRetrievePort,
    private val relaxer: GroundedConditionRelaxer,
    @Value("\${app.grounded-search.answer.min-score:0}")
    private val minScore: Double,
) {
    fun answer(query: String): GroundedAnswer {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "검색어는 비어 있을 수 없습니다." }
        val checked = GroundedConditionCheck.accept(extractPort.extract(trimmed))
        val scope = relaxer.resolve(checked)
        val retrieval = retrievePort.retrieve(trimmed, scope)
        return decide(trimmed, checked, scope, retrieval)
    }

    private fun decide(
        query: String,
        checked: GroundedChecked,
        scope: GroundedScope,
        retrieval: GroundedRetrieval,
    ): GroundedAnswer {
        val citations = labels(retrieval.hits)
        val products = catalogPort.facts(productIds(retrieval.hits, scope), scope.dateFrom, scope.dateTo)
        val verdict = verdict(query, retrieval, citations, products)
        return GroundedAnswer(
            answer = spoken(verdict, scope.askConfirm),
            citations = citations,
            products = products,
            extracted = checked,
            relaxed = scope.relaxed,
            keywordHits = retrieval.keywordHits,
            vectorHits = retrieval.vectorHits,
            rrf = retrieval.fused,
            rerankScores = retrieval.hits.map { it.rerankScore },
            verdict = verdict,
        )
    }

    private fun verdict(
        query: String,
        retrieval: GroundedRetrieval,
        citations: List<GroundedCitationPrompt>,
        products: List<GroundedProductFacts>,
    ): GroundedVerdict {
        val top = retrieval.hits.firstOrNull()?.let { it.rerankScore ?: it.rrfScore } ?: 0.toDouble()
        if (retrieval.hits.isEmpty() || top < minScore) {
            return GroundedVerdict(false, null, GroundedAnswerCheck.NO_EVIDENCE)
        }
        return GroundedAnswerCheck.judge(answerPort.answer(query, citations), citations.size, products)
    }

    private fun spoken(verdict: GroundedVerdict, askConfirm: Boolean): String? {
        if (!verdict.accepted) {
            return null
        }
        val text = verdict.text.orEmpty()
        return if (askConfirm) "조건을 확인해 주세요. $text" else text
    }

    private fun labels(hits: List<GroundedSearchHit>): List<GroundedCitationPrompt> =
        hits.mapIndexed { index, hit ->
            GroundedCitationPrompt("C${index + 1}", hit.chunkId, hit.linkKey, hit.content)
        }

    private fun productIds(hits: List<GroundedSearchHit>, scope: GroundedScope): Set<Long> =
        hits.mapNotNull { owner(it, scope) }.toSet()

    private fun owner(hit: GroundedSearchHit, scope: GroundedScope): Long? =
        when (hit.sourceType) {
            RagSourceType.PRODUCT -> hit.sourceId
            RagSourceType.OPTION -> scope.optionToProduct[hit.sourceId]
            else -> null
        }
}
