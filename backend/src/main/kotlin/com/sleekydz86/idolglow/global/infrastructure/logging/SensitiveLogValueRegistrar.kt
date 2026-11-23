package com.sleekydz86.idolglow.global.infrastructure.logging

import org.slf4j.LoggerFactory
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class SensitiveLogValueRegistrar(
    environment: ConfigurableEnvironment,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        val values = collectSecretValues(environment)
        SensitiveLogMasker.registerSecretValues(values)
        log.info("로그에서 가릴 설정 값을 등록했습니다. count={}", values.size)
    }

    companion object {
        private const val MIN_SECRET_LENGTH: Int = 8
        private val SECRET_NAME = Regex("(?i)(key|secret|token|password|passwd|webhook|credential)")

        fun collectSecretValues(environment: ConfigurableEnvironment): Set<String> =
            environment.propertySources
                .filterIsInstance<EnumerablePropertySource<*>>()
                .flatMap { it.propertyNames.asList() }
                .filter { SECRET_NAME.containsMatchIn(it) }
                .mapNotNull { runCatching { environment.getProperty(it) }.getOrNull()?.trim() }
                .filter(::looksLikeSecret)
                .flatMap { listOf(it, URLEncoder.encode(it, StandardCharsets.UTF_8)) }
                .toSet()

        private fun looksLikeSecret(value: String): Boolean =
            value.length >= MIN_SECRET_LENGTH &&
                !value.all { it.isDigit() } &&
                !value.equals("true", ignoreCase = true) &&
                !value.equals("false", ignoreCase = true)
    }
}
