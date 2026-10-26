package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import java.math.BigDecimal

object GroundedEvalScore {
    fun cutoff(): Int = (2 shl 1) + 1

    fun recallAt(ranked: List<String>, expected: Set<String>, cutoff: Int): Double {
        if (expected.isEmpty()) {
            return 0.toDouble()
        }
        val hit = ranked.take(cutoff).count { it in expected }
        return hit.toDouble() / expected.size.toDouble()
    }

    fun reciprocalRank(ranked: List<String>, expected: Set<String>): Double {
        val index = ranked.indexOfFirst { it in expected }
        if (index < 0) {
            return 0.toDouble()
        }
        return 1.toDouble() / (index + 1).toDouble()
    }

    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) {
            return 0.toDouble()
        }
        return values.sum() / values.size.toDouble()
    }

    fun same(actual: GroundedChecked, expected: GroundedChecked): Map<String, Boolean> =
        mapOf(
            "region" to (actual.region == expected.region),
            "dateFrom" to (actual.dateFrom == expected.dateFrom),
            "dateTo" to (actual.dateTo == expected.dateTo),
            "maxBudget" to sameMoney(actual.maxBudget, expected.maxBudget),
            "category" to (actual.category == expected.category),
            "people" to (actual.people == expected.people),
        )

    private fun sameMoney(actual: BigDecimal?, expected: BigDecimal?): Boolean {
        if (actual == null || expected == null) {
            return actual == null && expected == null
        }
        return actual.compareTo(expected) == 0
    }
}
