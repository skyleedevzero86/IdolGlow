package com.sleekydz86.idolglow.global.infrastructure.logging

import java.util.concurrent.atomic.AtomicReference

object SensitiveLogMasker {
    const val MASK: String = "****"

    private const val SECRET_NAMES: String =
        "service_?key|api[-_]?key|auth_?key|access_?token|refresh_?token|id_?token|client_?secret|" +
            "secret_?key|secret|password|passwd|billing_?key|webhook_?secret|jsessionid|token"

    private val patternRules: List<Pair<Regex, String>> =
        listOf(
            Regex("(?i)\\b(bearer|basic)\\s+[A-Za-z0-9\\-._~+/]+=*") to "$1 $MASK",
            Regex("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}") to MASK,
            Regex("(?i)(\"(?:$SECRET_NAMES)\"\\s*:\\s*\")[^\"]*(\")") to "$1$MASK$2",
            Regex("(?i)\\b((?:$SECRET_NAMES)\\s*[=:]\\s*)[^\\s&,;\"'<>)}\\]]+") to "$1$MASK",
            Regex("(?i)(api\\.telegram\\.org/bot)[^/\\s]+") to "$1$MASK",
            Regex("(?i)(discord(?:app)?\\.com/api/webhooks/)[^\\s\"']+") to "$1$MASK",
        )

    private val secretValues = AtomicReference<List<String>>(emptyList())

    fun registerSecretValues(values: Collection<String>) {
        secretValues.updateAndGet { current ->
            (current + values).filter { it.isNotBlank() }.distinct().sortedByDescending { it.length }
        }
    }

    fun mask(text: String?): String? {
        if (text.isNullOrEmpty()) return text
        val byValue = secretValues.get().fold(text) { acc, secret -> acc.replace(secret, MASK) }
        return patternRules.fold(byValue) { acc, (regex, replacement) -> regex.replace(acc, replacement) }
    }
}
