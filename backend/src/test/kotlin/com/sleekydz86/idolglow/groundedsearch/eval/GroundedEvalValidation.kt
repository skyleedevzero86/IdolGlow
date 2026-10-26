package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswerCheck
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedOptionPrice
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSlotFact
import java.time.LocalTime

object GroundedEvalValidation {
    fun rejectedCount(probes: List<GroundedEvalProbe>): Int = probes.count(::rejected)

    private fun rejected(probe: GroundedEvalProbe): Boolean =
        !GroundedAnswerCheck.judge(probe.answer, probe.citationCount, listOf(facts(probe))).accepted

    private fun facts(probe: GroundedEvalProbe): GroundedProductFacts {
        val start = LocalTime.of((2 shl 2) + 1, 0)
        return GroundedProductFacts(
            productId = 1L,
            name = "평가 상품",
            minPrice = probe.minPrice,
            options = listOf(GroundedOptionPrice(optionId = 1L, name = "평가 옵션", price = probe.optionPrice)),
            slots = listOf(GroundedSlotFact(date = probe.slotDate, start = start, end = start.plusHours(1))),
        )
    }
}
