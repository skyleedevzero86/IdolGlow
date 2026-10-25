package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import java.math.BigDecimal
import java.time.LocalDate

data class GroundedEvalSource(
    val type: RagSourceType,
    val id: Long,
    val content: String,
)

data class GroundedEvalCase(
    val question: String,
    val language: String,
    val expected: GroundedChecked,
    val sources: List<GroundedEvalSource>,
)

data class GroundedEvalProbe(
    val answer: String,
    val citationCount: Int,
    val minPrice: BigDecimal,
    val optionPrice: BigDecimal,
    val slotDate: LocalDate,
)

data class GroundedEvalModeScore(
    val mode: String,
    val recall: Double,
    val mrr: Double,
)

data class GroundedEvalResult(
    val database: String?,
    val skippedReason: String?,
    val modes: List<GroundedEvalModeScore>,
    val skippedModes: List<String>,
    val fieldRates: Map<String, Double>?,
    val rejected: Int,
    val probeCount: Int,
    val caseCount: Int,
)
