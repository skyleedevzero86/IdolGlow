package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedConditionDraft(
    val region: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val maxBudget: String? = null,
    val category: String? = null,
    val people: String? = null,
    val failed: Boolean = false,
)
