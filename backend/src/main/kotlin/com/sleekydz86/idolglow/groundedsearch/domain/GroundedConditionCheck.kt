package com.sleekydz86.idolglow.groundedsearch.domain

import com.sleekydz86.idolglow.productpackage.attraction.domain.SeoulDistrictTourCodeMapper
import java.math.BigDecimal
import java.time.LocalDate

object GroundedConditionCheck {
    fun accept(draft: GroundedConditionDraft): GroundedChecked {
        val ignored = mutableListOf<String>()
        val region = region(draft.region, ignored)
        val range = dates(draft.dateFrom, draft.dateTo, ignored)
        val budget = budget(draft.maxBudget, ignored)
        val people = people(draft.people, ignored)
        if (draft.failed) {
            ignored += "추출"
        }
        return GroundedChecked(
            region = region,
            dateFrom = range?.first,
            dateTo = range?.second,
            maxBudget = budget,
            category = text(draft.category),
            people = people,
            ignored = ignored,
        )
    }

    private fun region(raw: String?, ignored: MutableList<String>): String? {
        val value = text(raw)
        val known = value != null && SeoulDistrictTourCodeMapper.signguCodeOf(value) != null
        if (value != null && !known) {
            ignored += "지역"
        }
        return if (known) value else null
    }

    private fun dates(fromRaw: String?, toRaw: String?, ignored: MutableList<String>): Pair<LocalDate, LocalDate>? {
        val from = parse(fromRaw)
        val to = parse(toRaw)
        val start = if (!provided(fromRaw, toRaw) || rejected(fromRaw, toRaw, from, to)) null else from ?: to
        if (provided(fromRaw, toRaw) && start == null) {
            ignored += "날짜"
        }
        return start?.let { it to (to ?: it) }
    }

    private fun provided(fromRaw: String?, toRaw: String?): Boolean = text(fromRaw) != null || text(toRaw) != null

    private fun rejected(fromRaw: String?, toRaw: String?, from: LocalDate?, to: LocalDate?): Boolean {
        val fromBad = text(fromRaw) != null && from == null
        val toBad = text(toRaw) != null && to == null
        return fromBad || toBad || reversed(from, to)
    }

    private fun reversed(from: LocalDate?, to: LocalDate?): Boolean {
        if (from == null || to == null) {
            return false
        }
        return from.isAfter(to)
    }

    private fun budget(raw: String?, ignored: MutableList<String>): BigDecimal? {
        val value = text(raw)
        val amount = value?.replace(",", "")?.removeSuffix("원")?.trim()?.toBigDecimalOrNull()
        val accepted = amount != null && amount >= BigDecimal.ZERO
        if (value != null && !accepted) {
            ignored += "예산"
        }
        return if (accepted) amount else null
    }

    private fun people(raw: String?, ignored: MutableList<String>): Int? {
        val value = text(raw)
        val count = value?.toIntOrNull()
        val accepted = count != null && count >= 1
        if (value != null && !accepted) {
            ignored += "인원"
        }
        return if (accepted) count else null
    }

    private fun parse(raw: String?): LocalDate? = text(raw)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun text(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }
}
