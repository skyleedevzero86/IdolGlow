package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.AnswerGeneratePort
import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedCatalogPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedRetrievePort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswerCheck
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionDraft
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedFilter
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedOptionPrice
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedRetrieval
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedScope
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("근거 검색 답변")
class GroundedAnswerServiceTest {
    @Test
    @DisplayName("결과가 없으면 푼 조건을 보여 준다")
    fun relaxedConditionIsShownWhenNoResult() {

        // given
        val day = LocalDate.now()
        val catalog = RecordingCatalog(listOf(emptySet(), emptySet(), emptySet(), setOf(1L)))
        val service = service(FixedExtract(valid(day)), FixedAnswer("[C1]"), catalog, emptyRetrieval(), 0.toDouble())

        // when
        val answer = service.answer("홍대 메이크업")

        // then
        assertEquals(listOf("날짜", "예산", "지역"), answer.relaxed)
        val slack = 2L + 1L
        assertEquals(day.minusDays(slack), catalog.seen[1].dateFrom)
        assertEquals(day.plusDays(slack), catalog.seen[1].dateTo)
        assertNull(catalog.seen[2].maxBudget)
        assertNull(catalog.seen[3].region)
    }

    @Test
    @DisplayName("답의 가격이 옵션과 다르면 거절한다")
    fun answerRejectedWhenPriceDiffersFromOption() {

        // given
        val facts =
            GroundedProductFacts(
                productId = 1L,
                name = "메이크업",
                minPrice = BigDecimal("39000"),
                options = listOf(GroundedOptionPrice(1L, "기본", BigDecimal("39000"))),
                slots = emptyList(),
            )
        val service =
            service(
                FixedExtract(GroundedConditionDraft()),
                FixedAnswer("35000원 [C1]"),
                FactCatalog(listOf(facts)),
                hitRetrieval(),
                0.toDouble(),
            )

        // when
        val answer = service.answer("메이크업 가격")

        // then
        assertNull(answer.answer)
        assertTrue(answer.verdict.rejectReason?.contains(GroundedAnswerCheck.VALUE_MISMATCH) == true)
    }

    @Test
    @DisplayName("인용 번호는 넘긴 문단 안에 있어야 한다")
    fun citationMustBeRetrievedChunk() {

        // given
        val service =
            service(
                FixedExtract(GroundedConditionDraft()),
                FixedAnswer("[C2]"),
                FactCatalog(emptyList()),
                hitRetrieval(),
                0.toDouble(),
            )

        // when
        val answer = service.answer("인용")

        // then
        assertNull(answer.answer)
        assertTrue(answer.verdict.rejectReason?.contains("인용") == true)
    }

    @Test
    @DisplayName("검색 점수가 낮으면 근거를 찾지 못했다고 한다")
    fun noAnswerWhenRetrievalScoreLow() {

        // given
        val service =
            service(
                FixedExtract(GroundedConditionDraft()),
                ThrowingAnswer(),
                FactCatalog(emptyList()),
                lowRetrieval(),
                1.toDouble(),
            )

        // when
        val answer = service.answer("낮은 점수")

        // then
        assertNull(answer.answer)
        assertEquals(GroundedAnswerCheck.NO_EVIDENCE, answer.verdict.rejectReason)
    }

    private fun service(
        extract: ConditionExtractPort,
        answer: AnswerGeneratePort,
        catalog: GroundedCatalogPort,
        retrieval: GroundedRetrieval,
        minScore: Double,
    ): GroundedAnswerService =
        GroundedAnswerService(
            extract,
            answer,
            catalog,
            FixedRetrieve(retrieval),
            GroundedConditionRelaxer(catalog),
            minScore,
        )

    private fun valid(day: LocalDate): GroundedConditionDraft =
        GroundedConditionDraft(region = "강남구", dateFrom = day.toString(), dateTo = day.toString(), maxBudget = "1")

    private fun emptyRetrieval(): GroundedRetrieval =
        GroundedRetrieval(emptyList(), emptyList(), emptyList(), emptyList())

    private fun hitRetrieval(): GroundedRetrieval =
        GroundedRetrieval(listOf(hit(1.toDouble())), listOf(1L), listOf(1L), emptyList())

    private fun lowRetrieval(): GroundedRetrieval =
        GroundedRetrieval(listOf(hit(0.toDouble())), listOf(1L), emptyList(), emptyList())

    private fun hit(score: Double): GroundedSearchHit =
        GroundedSearchHit(1L, RagSourceType.PRODUCT, 1L, "product/1", "메이크업", 1, 1, score)
}

private class FixedExtract(private val draft: GroundedConditionDraft) : ConditionExtractPort {
    override fun extract(query: String): GroundedConditionDraft = draft
}

private class FixedAnswer(private val text: String) : AnswerGeneratePort {
    override fun answer(query: String, citations: List<GroundedCitationPrompt>): String = text
}

private class ThrowingAnswer : AnswerGeneratePort {
    override fun answer(query: String, citations: List<GroundedCitationPrompt>): String =
        error("점수가 낮으면 답변을 호출하지 않아야 합니다.")
}

private class FixedRetrieve(private val retrieval: GroundedRetrieval) : GroundedRetrievePort {
    override fun retrieve(query: String, scope: GroundedScope): GroundedRetrieval = retrieval
}

private class FactCatalog(private val rows: List<GroundedProductFacts>) : GroundedCatalogPort {
    override fun match(filter: GroundedFilter): Set<Long> = emptySet()

    override fun optionOwners(productIds: Set<Long>): Map<Long, Long> = emptyMap()

    override fun facts(
        productIds: Set<Long>,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
    ): List<GroundedProductFacts> = rows
}

private class RecordingCatalog(private val matches: List<Set<Long>>) : GroundedCatalogPort {
    val seen = mutableListOf<GroundedFilter>()
    private var index = 0

    override fun match(filter: GroundedFilter): Set<Long> {
        seen += filter
        val ids = matches[index]
        index += 1
        return ids
    }

    override fun optionOwners(productIds: Set<Long>): Map<Long, Long> = emptyMap()

    override fun facts(
        productIds: Set<Long>,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
    ): List<GroundedProductFacts> = emptyList()
}
