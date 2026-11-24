package com.sleekydz86.idolglow.groundedsearch.infrastructure

import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@Component
class GroundedRemoteSettings(
    private val environment: Environment,
) {
    fun text(key: String): String = environment.getProperty(key).orEmpty().trim()

    fun llmTimeout(): Duration = seconds(LLM_TIMEOUT, "근거 검색 언어 모델 제한 시간이 없습니다.")

    fun rerankTimeout(): Duration = seconds(RERANK_TIMEOUT, "근거 검색 재정렬 제한 시간이 없습니다.")

    private fun seconds(key: String, message: String): Duration {
        val value = environment.getProperty(key, Long::class.java)
        if (value == null || value <= 0L) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, message)
        }
        return Duration.ofSeconds(value)
    }

    private companion object {
        private const val LLM_TIMEOUT = "app.grounded-search.llm.timeout-seconds"
        private const val RERANK_TIMEOUT = "app.grounded-search.rerank.timeout-seconds"
    }
}
