package com.sleekydz86.idolglow.global.llm

import org.springframework.stereotype.Component

@Component
class LlmInputMasker {
    fun mask(text: String): String =
        RULES.fold(text) { acc, (pattern, replacement) ->
            pattern.replace(acc, replacement)
        }

    companion object {
        private val RULES: List<Pair<Regex, String>> =
            listOf(
                Regex("sk-ant-[A-Za-z0-9_-]{16,}") to "sk-ant-***",
                Regex("sk-[A-Za-z0-9_-]{16,}") to "sk-***",
                Regex("gh[pousr]_[A-Za-z0-9]{20,}") to "gh*_***",
                Regex("(?i)Bearer\\s+\\S+") to "Bearer ***",
                Regex("(?i)((?:password|passwd|secret|token|api[-_]?key)\\s*[=:]\\s*)\\S+") to "$1***",
                Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+") to "<email>",
                Regex("\\b\\d{6}-\\d{7}\\b") to "<rrn>",
                Regex("\\b01[016789]-?\\d{3,4}-?\\d{4}\\b") to "<phone>",
            )
    }
}
