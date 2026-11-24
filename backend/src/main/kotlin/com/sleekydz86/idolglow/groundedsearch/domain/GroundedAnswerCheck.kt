package com.sleekydz86.idolglow.groundedsearch.domain

import java.math.BigDecimal
import java.time.LocalDate

object GroundedAnswerCheck {
    const val VALUE_MISMATCH: String = "답의 값 ≠ 기준 값(출처)"
    const val NO_EVIDENCE: String = "근거를 찾지 못했다"

    fun judge(answer: String?, citationCount: Int, facts: List<GroundedProductFacts>): GroundedVerdict {
        val text = answer?.trim().orEmpty()
        val reason =
            when {
                text.isEmpty() -> "답변을 만들지 못했습니다."
                else -> citationReason(text, citationCount) ?: valueReason(text, facts)
            }
        return if (reason == null) GroundedVerdict(true, text, null) else GroundedVerdict(false, null, reason)
    }

    private fun citationReason(text: String, citationCount: Int): String? {
        val numbers = CITATION.findAll(text).map { it.groupValues[1].toInt() }.toList()
        if (numbers.any { it < 1 || it > citationCount }) {
            return "인용 번호가 검색된 문단 밖에 있습니다."
        }
        return null
    }

    private fun valueReason(text: String, facts: List<GroundedProductFacts>): String? {
        val prices = PRICE.findAll(text).map { amount(it.groupValues[1]) }.toList()
        val dates = DATE.findAll(text).map { LocalDate.parse(it.value) }.toList()
        val priceMismatch = prices.any { price -> allowedPrices(facts).none { it.compareTo(price) == 0 } }
        val dateMismatch = dates.any { date -> allowedDates(facts).none { it == date } }
        return if (priceMismatch || dateMismatch) VALUE_MISMATCH else null
    }

    private fun allowedPrices(facts: List<GroundedProductFacts>): List<BigDecimal> =
        facts.flatMap { fact -> listOf(fact.minPrice) + fact.options.map { it.price } }

    private fun allowedDates(facts: List<GroundedProductFacts>): List<LocalDate> =
        facts.flatMap { fact -> fact.slots.map { it.date } }

    private fun amount(raw: String): BigDecimal = BigDecimal(raw.replace(",", ""))

    private val CITATION = Regex("\\[C(\\d+)\\]")
    private val PRICE = Regex("(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*원")
    private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
}
