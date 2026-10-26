package com.sleekydz86.idolglow.groundedsearch.eval

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

object GroundedEvalReport {
    const val KEYWORD: String = "키워드만"
    const val VECTOR: String = "벡터만"
    const val HYBRID: String = "Hybrid"
    const val RERANK: String = "Hybrid+Rerank"
    private val MODES = listOf(KEYWORD, VECTOR, HYBRID, RERANK)
    private val FIELDS = listOf("region", "dateFrom", "dateTo", "maxBudget", "category", "people")

    fun write(result: GroundedEvalResult) {
        val path = Path.of("build", "reports", "grounded-search-eval.md")
        Files.createDirectories(path.parent)
        val text = body(result)
        Files.writeString(path, text, StandardCharsets.UTF_8)
        println(text)
    }

    private fun body(result: GroundedEvalResult): String =
        buildString {
            appendLine("# 근거 검색 평가")
            appendLine()
            appendLine("골든셋 예시는 ${result.caseCount}개다. 50~100개로 늘리는 나머지는 사용자가 채운다.")
            appendLine()
            skipped(result)?.let {
                appendLine(it)
                appendLine()
            }
            appendLine("## 검색")
            appendLine()
            append(searchTable(result))
            appendLine()
            appendLine("## 조건 추출")
            appendLine()
            append(fieldTable(result))
            appendLine()
            appendLine("## 검증")
            appendLine()
            append(validationTable(result))
            appendLine()
            appendLine("Recall@5는 상위 ${GroundedEvalScore.cutoff()}개 안에 정답 출처가 얼마나 들어왔는지다.")
            appendLine("MRR은 그 방식이 돌려준 순위 중 첫 정답의 역수 평균이다.")
        }

    private fun skipped(result: GroundedEvalResult): String? = result.skippedReason

    private fun searchTable(result: GroundedEvalResult): String {
        val database = result.database ?: "측정 안 함"
        val byMode = result.modes.associateBy { it.mode }
        val lines =
            MODES.map { mode ->
                val score = byMode[mode]
                if (score == null) {
                    "| $database | $mode | 건너뜀 | 건너뜀 |"
                } else {
                    "| $database | $mode | ${num(score.recall)} | ${num(score.mrr)} |"
                }
            }
        return "| DB | 방식 | Recall@5 | MRR |\n| --- | --- | --- | --- |\n${lines.joinToString("\n")}\n"
    }

    private fun fieldTable(result: GroundedEvalResult): String {
        val lines =
            FIELDS.map { field ->
                val rate = result.fieldRates?.get(field)
                "| $field | ${rate?.let(::num) ?: "건너뜀"} |"
            }
        return "| 필드 | 정확도 |\n| --- | --- |\n${lines.joinToString("\n")}\n"
    }

    private fun validationTable(result: GroundedEvalResult): String =
        "| DB | 틀린 답 | 거절 |\n| --- | --- | --- |\n| 코드 | ${result.probeCount} | ${result.rejected} |\n"

    private fun num(value: Double): String = String.format(Locale.US, "%.4f", value)
}
