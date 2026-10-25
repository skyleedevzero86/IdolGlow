package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import java.math.BigDecimal

data class GroundedProductAnswerResponse(
    val productId: Long,
    val name: String,
    val minPrice: BigDecimal,
    val options: List<GroundedOptionAnswerResponse>,
    val slots: List<GroundedSlotResponse>,
) {
    companion object {
        fun from(facts: GroundedProductFacts): GroundedProductAnswerResponse =
            GroundedProductAnswerResponse(
                productId = facts.productId,
                name = facts.name,
                minPrice = facts.minPrice,
                options = facts.options.map { GroundedOptionAnswerResponse(it.optionId, it.name, it.price) },
                slots = facts.slots.map { GroundedSlotResponse(it.date, it.start, it.end) },
            )
    }
}
