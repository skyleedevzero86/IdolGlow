package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate

object GroundedGoldenParser {
    private val mapper = ObjectMapper()

    fun case(line: String): GroundedEvalCase {
        val node = mapper.readTree(line)
        return GroundedEvalCase(
            question = required(node, "question"),
            language = required(node, "language"),
            expected = expected(node.path("expected")),
            sources = sources(node.path("sources")),
        )
    }

    fun probe(line: String): GroundedEvalProbe {
        val node = mapper.readTree(line)
        return GroundedEvalProbe(
            answer = required(node, "answer"),
            citationCount = node.path("citationCount").asInt(),
            minPrice = money(required(node, "minPrice")),
            optionPrice = money(required(node, "optionPrice")),
            slotDate = day(required(node, "slotDate")),
        )
    }

    fun sourceKey(type: RagSourceType, id: Long): String = "${type.name}:$id"

    private fun expected(node: JsonNode): GroundedChecked =
        GroundedChecked(
            region = optional(node, "region"),
            dateFrom = optional(node, "dateFrom")?.let(::day),
            dateTo = optional(node, "dateTo")?.let(::day),
            maxBudget = optional(node, "maxBudget")?.let(::money),
            category = optional(node, "category"),
            people = optional(node, "people")?.let(::count),
            ignored = emptyList(),
        )

    private fun sources(node: JsonNode): List<GroundedEvalSource> {
        if (!node.isArray || node.isEmpty) {
            throw IllegalArgumentException("정답 출처가 없습니다.")
        }
        return (0 until node.size()).map { index -> source(node.get(index)) }
    }

    private fun source(node: JsonNode): GroundedEvalSource =
        GroundedEvalSource(
            type = RagSourceType.valueOf(required(node, "type")),
            id = node.path("id").asLong(),
            content = required(node, "content"),
        )

    private fun required(node: JsonNode, field: String): String =
        optional(node, field) ?: throw IllegalArgumentException("평가 항목에 $field 가 없습니다.")

    private fun optional(node: JsonNode, field: String): String? {
        val child = node.path(field)
        if (child.isMissingNode || child.isNull) {
            return null
        }
        return child.asText().trim().ifEmpty { null }
    }

    private fun money(raw: String): BigDecimal =
        raw.toBigDecimalOrNull() ?: throw IllegalArgumentException("예산은 숫자여야 합니다. value=$raw")

    private fun day(raw: String): LocalDate =
        runCatching { LocalDate.parse(raw) }.getOrElse {
            throw IllegalArgumentException("날짜는 yyyy-MM-dd 여야 합니다. value=$raw")
        }

    private fun count(raw: String): Int =
        raw.toIntOrNull() ?: throw IllegalArgumentException("인원은 정수여야 합니다. value=$raw")
}
