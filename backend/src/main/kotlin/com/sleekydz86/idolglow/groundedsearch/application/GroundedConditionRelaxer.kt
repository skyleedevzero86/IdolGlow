package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedCatalogPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedFilter
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedScope
import org.springframework.stereotype.Component

@Component
class GroundedConditionRelaxer(
    private val catalogPort: GroundedCatalogPort,
) {
    fun resolve(checked: GroundedChecked): GroundedScope {
        if (!checked.hasConstraint()) {
            return GroundedScope.unfiltered().copy(askConfirm = checked.ignored.isNotEmpty())
        }
        return loosen(checked.toFilter())
    }

    private fun loosen(start: GroundedFilter): GroundedScope {
        var filter = start
        var ids = catalogPort.match(filter)
        val relaxed = mutableListOf<String>()
        STEPS.forEach { step ->
            if (ids.isEmpty()) {
                val next = step.loosen(filter)
                if (next != filter) {
                    filter = next
                    relaxed += step.label
                    ids = catalogPort.match(filter)
                }
            }
        }
        return GroundedScope(ids, catalogPort.optionOwners(ids), relaxed, filter.dateFrom, filter.dateTo, false)
    }

    private enum class Step(val label: String) {
        DATE("날짜") {
            override fun loosen(filter: GroundedFilter): GroundedFilter = filter.widenDates()
        },
        BUDGET("예산") {
            override fun loosen(filter: GroundedFilter): GroundedFilter =
                if (filter.maxBudget == null) filter else filter.copy(maxBudget = null)
        },
        REGION("지역") {
            override fun loosen(filter: GroundedFilter): GroundedFilter =
                if (filter.region == null) filter else filter.copy(region = null)
        },
        ;

        abstract fun loosen(filter: GroundedFilter): GroundedFilter
    }

    private companion object {
        private val STEPS = listOf(Step.DATE, Step.BUDGET, Step.REGION)
    }
}
