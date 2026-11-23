package com.sleekydz86.idolglow.groundedsearch.eval

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@Tag("eval")
@DisabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "\\S+")
@DisplayName("근거 검색 평가")
class GroundedSearchEvalTest {
    @Test
    @DisplayName("API 키가 없으면 검색 점수를 만들지 않는다")
    fun skipsWhenApiKeyMissing() {

        // given
        val cases = GroundedGoldenSet.cases()
        val probes = GroundedGoldenSet.probes()

        // when
        val result =
            GroundedEvalResult(
                database = null,
                skippedReason = "OPENAI_API_KEY가 없어 검색과 조건 추출은 측정하지 않았습니다.",
                modes = emptyList(),
                skippedModes = listOf(
                    GroundedEvalReport.KEYWORD,
                    GroundedEvalReport.VECTOR,
                    GroundedEvalReport.HYBRID,
                    GroundedEvalReport.RERANK,
                ),
                fieldRates = null,
                rejected = GroundedEvalValidation.rejectedCount(probes),
                probeCount = probes.size,
                caseCount = cases.size,
            )

        // then
        GroundedEvalReport.write(result)
    }
}
