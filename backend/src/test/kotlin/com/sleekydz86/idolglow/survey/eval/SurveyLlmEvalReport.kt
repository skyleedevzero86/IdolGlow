package com.sleekydz86.idolglow.survey.eval

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object SurveyLlmEvalReport {
    fun write(results: List<SurveyEvalProviderResult>) {
        val report = body(results)
        val path = Path.of("build", "reports", "survey-llm-eval", "report.md")
        Files.createDirectories(path.parent)
        Files.writeString(path, report, StandardCharsets.UTF_8)
        println(report)
    }

    private fun body(results: List<SurveyEvalProviderResult>): String =
        buildString {
            appendLine("# 설문 추천 LLM 품질 평가")
            appendLine()
            appendLine("| 공급자 | 형식 성공 | 목록 밖 추천 | 금지어 | 평균 시간 | 비용 |")
            appendLine("| --- | ---: | ---: | ---: | ---: | --- |")
            results.forEach { result -> appendLine(row(result)) }
            results.forEach { result ->
                appendLine()
                appendLine("## ${result.provider} 실패 샘플")
                appendLine()
                if (result.skippedReason != null) {
                    appendLine("건너뜀: ${result.skippedReason}")
                } else {
                    val failures = result.samples.filter { it.score.reasons.isNotEmpty() }
                    if (failures.isEmpty()) appendLine("없음")
                    failures.forEach { appendLine("- ${it.sampleNumber}번: ${it.score.reasons.joinToString(", ")}") }
                    failures.filter { it.responseText != null }.take(EXAMPLE_LIMIT).forEach { sample ->
                        appendLine()
                        appendLine("### ${sample.sampleNumber}번 응답 원문")
                        appendLine()
                        appendLine("```")
                        appendLine(sample.responseText?.take(EXAMPLE_TEXT_LIMIT))
                        appendLine("```")
                    }
                }
            }
        }

    private fun row(result: SurveyEvalProviderResult): String {
        if (result.skippedReason != null) return "| ${result.provider} | 건너뜀 | 건너뜀 | 건너뜀 | 건너뜀 | ${result.cost} |"
        val total = result.samples.size
        val formats = result.samples.count { it.score.formatSuccess }
        val outside = result.samples.count { it.score.outsideRecommendation }
        val forbidden = result.samples.count { it.score.forbiddenExpression }
        val average =
            result.samples
                .map { it.latencyMs }
                .average()
                .toLong()
        return "| ${result.provider} | $formats/$total | $outside | $forbidden | ${average}ms | ${result.cost} |"
    }

    private const val EXAMPLE_LIMIT: Int = 3
    private const val EXAMPLE_TEXT_LIMIT: Int = 1200
}
