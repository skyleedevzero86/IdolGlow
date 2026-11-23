package com.sleekydz86.idolglow.groundedsearch.infrastructure

import java.math.BigDecimal

class GroundedBudgetMatch(
    private val jpql: GroundedJpql,
) {
    fun ids(maxBudget: BigDecimal): Set<Long> {
        val bases = jpql.pairs("select p.id, p.basePrice from Product p")
        val options = jpql.pairs("select po.product.id, o.price from ProductOption po join po.option o")
        val byProduct = options.groupBy({ it.first }, { it.second })
        return bases.mapNotNull { (id, base) -> match(id, base, byProduct[id].orEmpty(), maxBudget) }.toSet()
    }

    private fun match(id: Long, base: BigDecimal, prices: List<BigDecimal>, maxBudget: BigDecimal): Long? {
        val min = if (prices.isEmpty()) base else base + prices.min()
        val matched = min <= maxBudget || prices.any { it <= maxBudget }
        return if (matched) id else null
    }
}
